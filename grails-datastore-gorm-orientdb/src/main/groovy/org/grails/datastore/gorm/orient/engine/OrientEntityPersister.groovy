package org.grails.datastore.gorm.orient.engine

import com.orientechnologies.orient.core.db.record.OIdentifiable
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.ORecord
import com.orientechnologies.orient.core.record.impl.ODocument
import com.tinkerpop.blueprints.impls.orient.OrientElement
import groovy.transform.CompileStatic
import groovy.util.logging.Log
import org.grails.datastore.gorm.GormEntity
import org.grails.datastore.gorm.orient.OrientPersistentEntity
import org.grails.datastore.gorm.orient.OrientSession
import org.grails.datastore.gorm.orient.collection.OrientPersistentList
import org.grails.datastore.gorm.orient.collection.OrientPersistentSet
import org.grails.datastore.gorm.orient.collection.OrientResultList
import org.grails.datastore.gorm.orient.collection.OrientList
import org.grails.datastore.gorm.orient.collection.OrientSet
import org.grails.datastore.gorm.orient.extensions.OrientGormHelper
import org.grails.datastore.gorm.orient.mapping.RelationshipUtils
import org.grails.datastore.mapping.collection.PersistentCollection
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.core.impl.PendingInsertAdapter
import org.grails.datastore.mapping.core.impl.PendingOperation
import org.grails.datastore.mapping.core.impl.PendingOperationAdapter
import org.grails.datastore.mapping.core.impl.PendingUpdateAdapter
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.datastore.mapping.dirty.checking.DirtyCheckableCollection
import org.grails.datastore.mapping.engine.AssociationQueryExecutor
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.engine.EntityPersister
import org.grails.datastore.mapping.engine.internal.MappingUtils
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.*
import org.grails.datastore.mapping.proxy.EntityProxy
import org.grails.datastore.mapping.query.Query
import org.springframework.context.ApplicationEventPublisher

/**
 * OrientDB entity persister implementation
 *
 * @author eugenekamenev
 */
@CompileStatic
@Log
class OrientEntityPersister extends EntityPersister {

    public OrientEntityPersister(MappingContext mappingContext, PersistentEntity entity, Session session, ApplicationEventPublisher publisher) {
        super(mappingContext, entity, session, publisher);
    }


    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity pe, Serializable[] keys) {
        def orientEntity = (OrientPersistentEntity) pe
        retrieveAllEntities(pe, keys as List<Serializable>)
    }

    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity pe, Iterable<Serializable> keys) {
        log.info("retrieveAllEntities called for $pe, $keys")
        def resultList = []
        def recordIdsList = []
        def containsInvalidKeys = OrientGormHelper.checkForRecordIds(keys.toList(), recordIdsList)
        if (!containsInvalidKeys) {
            log.info("no invalid keys found so trying to get entities with a query")
            resultList = createQuery().in(pe.getIdentity().name, recordIdsList).list()

        } else {
            log.info("parameter list contained new or invalid @rid, trying to load from first level cache")
            // seems that we should look at orientdb session
            resultList = recordIdsList.collect {
                orientDbSession().documentTx.load((ORecordId) it)
            }
        }
        new OrientResultList(0, (Iterator) resultList.iterator(), (OrientEntityPersister) this)
    }

    @Override
    protected List<Serializable> persistEntities(PersistentEntity pe,
                                                 @SuppressWarnings("rawtypes") Iterable objs) {
        def orientEntity = (OrientPersistentEntity) pe
        def collection = []
        for (object in objs) {
            collection << this.persistEntity(pe, object)
        }
        return collection
    }

    @Override
    protected Object retrieveEntity(PersistentEntity pe, Serializable key) {
        def orientEntity = (OrientPersistentEntity) pe
        def recordId = OrientGormHelper.createRecordId(key)
        if (!recordId) {
            return null
        }
        def record = recordId.record.load()
        return unmarshallEntity(persistentEntity, record)
    }

    @Override
    protected Serializable persistEntity(PersistentEntity pe, Object obj) {
        if (obj == null) {
            throw new IllegalStateException("Unable to persist null object");
        }
        if (shouldIgnore(orientDbSession(), obj)) {
            return (Serializable) orientDbSession().createEntityAccess(pe, obj).identifier
        }
        if (mappingContext.proxyFactory.isProxy(obj)) {
            return ((EntityProxy)obj).proxyKey
        }
        def orientEntity = (OrientPersistentEntity) pe
        def entityAccess = orientDbSession().createEntityAccess(pe, obj)
        orientDbSession().registerPending(obj)
        Object identifier = entityAccess.identifier
        boolean isUpdate = identifier != null && !(OrientGormHelper.createRecordId(identifier).isNew())
        if (isUpdate) {
            registerPendingUpdate(orientDbSession(), pe, entityAccess, obj, (Serializable) identifier)
        } else {
            OIdentifiable entityId = OrientGormHelper.createRecordId(entityAccess.identifier)
            if (entityId == null) {
                entityId = OrientGormHelper.createNewOrientEntry(orientEntity, obj, orientDbSession())
                // we need to save it right now, it should be attached to ODocumentTx
                entityId = OrientGormHelper.saveEntry(entityId)
                entityAccess.setIdentifierNoConversion(entityId)
            }
            final PendingInsertAdapter<Object, Serializable> pendingInsert = new PendingInsertAdapter<Object, Serializable>(pe, (Serializable) entityId, obj, entityAccess) {
                @Override
                public void run() {
                    if (cancelInsert(pe, entityAccess)) {
                        setVetoed(true);
                    }
                }
            };
            pendingInsert.addCascadeOperation(new PendingOperationAdapter<Object, Serializable>(pe, (Serializable) entityId, obj) {
                @Override
                public void run() {
                    firePostInsertEvent(pe, entityAccess);
                }
            });
            final List<PendingOperation<Object, Serializable>> preOperations = pendingInsert.getPreOperations();
            for (PendingOperation preOperation : preOperations) {
                preOperation.run();
            }

            pendingInsert.run();
            pendingInsert.setExecuted(true);

            if(pendingInsert.isVetoed()) {
                return null;
            }
            if (orientEntity.edge) {
                persistEdge(orientEntity, entityAccess)
            }

            orientDbSession().addPendingInsert(pendingInsert)
            persistAssociationsOfEntity(orientEntity, entityAccess, isUpdate)
            OrientGormHelper.saveEntry(marshallEntity(orientEntity, entityAccess))
            return entityId.record
        }
        return null
    }

    private void registerPendingUpdate(OrientSession session, final PersistentEntity pe, final EntityAccess entityAccess, final Object obj, final Serializable identifier) {
        final PendingUpdateAdapter<Object, Serializable> pendingUpdate = new PendingUpdateAdapter<Object, Serializable>(pe, identifier, obj, entityAccess) {
            @Override
            public void run() {
                if (cancelUpdate(pe, entityAccess)) {
                    setVetoed(true);
                }
            }
        };
        pendingUpdate.addCascadeOperation(new PendingOperationAdapter<Object, Serializable>(pe, identifier, obj) {
            @Override
            public void run() {
                firePostUpdateEvent(pe, entityAccess);
            }
        });
        session.addPendingUpdate(pendingUpdate);

        persistAssociationsOfEntity(pe, entityAccess, true);
    }

    private void persistAssociationsOfEntity(PersistentEntity pe, EntityAccess entityAccess, boolean isUpdate) {

        Object obj = entityAccess.getEntity();
        DirtyCheckable dirtyCheckable = null;
        if (obj instanceof DirtyCheckable) {
            dirtyCheckable = (DirtyCheckable)obj;
        }

        for (PersistentProperty pp: pe.getAssociations()) {
            if ((!isUpdate) || ((dirtyCheckable!=null) && dirtyCheckable.hasChanged(pp.getName()))) {

                Object propertyValue = entityAccess.getProperty(pp.getName());
                boolean isProxyInitilized = session.mappingContext.proxyFactory.isInitialized(propertyValue)
                if ((pp instanceof OneToMany) || (pp instanceof ManyToMany)) {
                    Association association = (Association) pp;

                    if (propertyValue!= null) {

                        if(propertyValue instanceof PersistentCollection) {
                            PersistentCollection pc = (PersistentCollection) propertyValue;
                            if(!pc.isInitialized()) continue;
                        }

                        if (association.isBidirectional()) {
                            // Populate other side of bidi
                            for (Object associatedObject: (Iterable)propertyValue) {
                                EntityAccess assocEntityAccess = createEntityAccess(association.getAssociatedEntity(), associatedObject);
                                String referencedPropertyName = association.getReferencedPropertyName();
                                if(association instanceof ManyToMany) {
                                    ((GormEntity)associatedObject).addTo(referencedPropertyName, obj);
                                }
                                else {
                                    assocEntityAccess.setPropertyNoConversion(referencedPropertyName, obj);
                                    ((DirtyCheckable)associatedObject).markDirty(referencedPropertyName);
                                }
                            }
                        }

                        Collection targets = (Collection) propertyValue;
                        persistEntities(association.getAssociatedEntity(), targets);

                        boolean reversed = RelationshipUtils.useReversedMappingFor(association);

                        if (!reversed) {
                            Collection dcc = createDirtyCheckableAwareCollection(entityAccess, association, targets);
                            entityAccess.setProperty(association.getName(), dcc);
                        }
                    }
                } else if (pp instanceof ToOne) {
                    if (propertyValue != null) {
                        ToOne to = (ToOne) pp;

                        if (to.isBidirectional()) {  // Populate other side of bidi
                            EntityAccess assocEntityAccess = createEntityAccess(to.getAssociatedEntity(), propertyValue);
                            if (to instanceof OneToOne) {
                                assocEntityAccess.setProperty(to.getReferencedPropertyName(), obj);
                            } else {
                                if(isProxyInitilized) {
                                    Collection collection = (Collection) assocEntityAccess.getProperty(to.getReferencedPropertyName());
                                    if (collection == null ) {
                                        collection = new ArrayList();
                                        assocEntityAccess.setProperty(to.getReferencedPropertyName(), collection);
                                    }
                                    if (!collection.contains(obj)) {
                                        collection.add(obj)
                                    }
                                }
                            }
                        }

                        persistEntity(to.getAssociatedEntity(), propertyValue);

                        boolean reversed = RelationshipUtils.useReversedMappingFor(to);

                        if (!reversed) {
                            final EntityAccess assocationAccess = orientDbSession().createEntityAccess(to.getAssociatedEntity(), propertyValue);
                            orientDbSession().addPendingRelationshipInsert((Serializable) entityAccess.getIdentifier(), to, (Serializable) assocationAccess.getIdentifier());
                        }
                    }
                } else {
                    throw new IllegalArgumentException("wtf don't know how to handle " + pp + "(" + pp.getClass() +")" );

                }
            }
        }
    }

    private Collection createCollection(Association association) {
        return association.isList() ? new ArrayList() : new HashSet();
    }

    private Collection createDirtyCheckableAwareCollection(EntityAccess entityAccess, Association association, Collection delegate) {
        if (delegate==null) {
            delegate = createCollection(association);
        }

        if( !(delegate instanceof DirtyCheckableCollection)) {

            final Object entity = entityAccess.getEntity();
            if(entity instanceof DirtyCheckable) {
                final OrientSession session = orientDbSession();
                for( Object o : delegate ) {
                    final EntityAccess associationAccess = session.createEntityAccess(association.getAssociatedEntity(), o);
                    println "called pending relationship insert inside createDirtyCheckableAwareCollection"
                    session.addPendingRelationshipInsert((Serializable) entityAccess.getIdentifier(), association, (Serializable) associationAccess.getIdentifier());
                }
                delegate = association.isList() ?
                        new OrientList(entityAccess, association,  (List) delegate, session) :
                        new OrientSet(entityAccess, association,  (Set) delegate, session);
            }
        }
        else {
            final DirtyCheckableCollection dirtyCheckableCollection = (DirtyCheckableCollection) delegate;
            final OrientSession session = orientDbSession();
            if(dirtyCheckableCollection.hasChanged()) {
                for (Object o : ((Iterable)dirtyCheckableCollection)) {
                    println "add pending insert for $o"
                    final EntityAccess associationAccess = orientDbSession().createEntityAccess(association.getAssociatedEntity(), o);
                    session.addPendingRelationshipInsert((Serializable) entityAccess.getIdentifier(),association, (Serializable) associationAccess.getIdentifier());
                }
            }
        }
        return delegate;
    }

    /**
     * Handling edge persist
     *
     * @param pe
     * @param object
     * @return
     */
    protected void persistEdge(OrientPersistentEntity pe, EntityAccess entityAccess) {
        def inAssociation = session.persist(entityAccess.getProperty('in'))
        def outAssociation = session.persist(entityAccess.getProperty('out'))
        def inVertex = orientDbSession().graph.getVertex(((OIdentifiable) inAssociation).identity)
        def outVertex = orientDbSession().graph.getVertex(((OIdentifiable) outAssociation).identity)
        def edge = orientDbSession().graph.addEdge("class:$pe.className", outVertex, inVertex, pe.className)
        entityAccess.setIdentifierNoConversion(edge.identity)
    }

    @Override
    protected void deleteEntity(PersistentEntity pe, Object obj) {
        def orientEntity = (OrientPersistentEntity) pe
        def identity = orientDbSession().createEntityAccess(orientEntity, obj).getIdentifier()
        OrientGormHelper.createRecordId(identity).record.delete()
    }

    @Override
    protected void deleteEntities(PersistentEntity pe,
                                  @SuppressWarnings("rawtypes") Iterable objects) {
        def orientEntity = (OrientPersistentEntity) pe
        for (object in objects) {
            def identity = orientDbSession().createEntityAccess(pe, object).getIdentifier()
            OrientGormHelper.createRecordId(identity).record.delete()
        }
    }

    @Override
    Serializable refresh(Object o) {
        println "refresh called"
        return null
    }

    OrientSession orientDbSession() {
        (OrientSession) session
    }

    @Override
    OrientPersistentEntity getPersistentEntity() {
        return (OrientPersistentEntity) super.getPersistentEntity()
    }

    OIdentifiable marshallEntity(OrientPersistentEntity entity, EntityAccess entityAccess) {
        OIdentifiable nativeObject = ((OIdentifiable) entityAccess.identifier).record
        for (property in entity.getPersistentProperties()) {
            if (property.name == 'in' || property.name == 'out' && entity.edge) continue;
            if(property instanceof Association) continue;
            OrientPersistentPropertyConverter.getForPersistentProperty(property).marshall(nativeObject, property, entityAccess, orientDbSession())
        }
        nativeObject
    }

    Object unmarshallEntity(OrientPersistentEntity entity, OIdentifiable nativeEntry) {
        if (nativeEntry == null) return nativeEntry;
        if (OrientGormHelper.getOrientClassName(nativeEntry) != null) {
            EntityAccess entityAccess = createEntityAccess(entity, entity.newInstance());
            OrientPersistentPropertyConverter.getBasic(Identity).unmarshall(nativeEntry, entity.identity, entityAccess, orientDbSession())
            orientDbSession().cacheEntry(entity, nativeEntry.identity, entityAccess.entity)
            final Object instance = entityAccess.getEntity();
            entityAccess.setIdentifierNoConversion(nativeEntry.identity)
            for (property in entityAccess.persistentEntity.getPersistentProperties()) {
                if(property instanceof ToOne) {

                    // if a lazy proxy should be created for this association then create it,
                    // note that this strategy does not allow for null checks
                    def associationQueryExecutor = new OrientAssociationQueryExecutor(nativeEntry.identity, (ToOne)property, session);
              //      if(property.getMapping().getMappedForm().isLazy()) {
                        final Object proxy = getMappingContext().getProxyFactory().createProxy(
                                this.session,
                                (AssociationQueryExecutor) associationQueryExecutor,
                                (Serializable)OrientPersistentPropertyConverter.getValue(nativeEntry, property, OType.LINK)
                        );
                        entityAccess.setPropertyNoConversion(property.name,
                                proxy
                        );
                    /*}
                    else {
                        final List<Object> results = associationQueryExecutor.query(nativeEntry.identity);
                        if(!results.isEmpty()) {
                            entityAccess.setPropertyNoConversion(MappingUtils.getTargetKey(property), results.get(0));
                        }
                    }*/
                }
                else if(property instanceof ToMany) {
                    Collection values;
                    final Class type = property.getType();
                    if(List.class.isAssignableFrom(type)) {
                        values = new OrientPersistentList(nativeEntry.identity, orientDbSession(), entityAccess, (ToMany) property);
                    } else {
                        values = new OrientPersistentSet(nativeEntry.identity, orientDbSession(), entityAccess, (ToMany) property);
                    }
                    entityAccess.setPropertyNoConversion(property.name, values);
                } else {
                    OrientPersistentPropertyConverter.getForPersistentProperty(property).unmarshall(nativeEntry, property, entityAccess, orientDbSession())
                }
            }
            firePostLoadEvent(entityAccess.getPersistentEntity(), entityAccess);
            return instance
        }
        return null
    }

    private boolean shouldIgnore(OrientSession session, Object obj) {
        boolean isDirty = obj instanceof DirtyCheckable ? ((DirtyCheckable)obj).hasChanged() : true;
        return session.isPendingAlready(obj) || (!isDirty);
    }

    Object unmarshallFromGraph(OrientPersistentEntity entity, OrientElement element) {
        throw new IllegalAccessException("Not yet implemented")
    }

    ORecord getRecord(Object recordId) {
        return OrientGormHelper.createRecordId(recordId).identity.record
    }

    @Override
    Query createQuery() {
        new OrientQuery(session, persistentEntity)
    }
}
