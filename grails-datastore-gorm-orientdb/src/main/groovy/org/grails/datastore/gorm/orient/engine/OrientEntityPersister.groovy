package org.grails.datastore.gorm.orient.engine

import com.orientechnologies.orient.core.db.record.OIdentifiable
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.record.ORecord
import com.orientechnologies.orient.core.record.impl.ODocument
import com.tinkerpop.blueprints.impls.orient.OrientElement
import grails.orientdb.OrientEntity
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.orient.OrientPersistentEntity
import org.grails.datastore.gorm.orient.OrientDbSession
import org.grails.datastore.gorm.orient.collection.OrientLinkedSet
import org.grails.datastore.gorm.orient.collection.OrientPersistentSet
import org.grails.datastore.gorm.orient.extensions.OrientGormHelper
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.engine.AssociationQueryExecutor
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.engine.EntityPersister
import org.grails.datastore.mapping.engine.Persister
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.types.*
import org.grails.datastore.mapping.proxy.ProxyFactory
import org.grails.datastore.mapping.query.Query
import org.springframework.context.ApplicationEventPublisher

@CompileStatic
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
        def orientEntity = (OrientPersistentEntity) pe
        def collection = []
        for (key in keys) {
            def nativeKey = createRecordIdWithKey(key)
            if (nativeKey) {
                collection << retrieveEntity(pe, key)
            }
        }
        return collection
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
        def recordId = createRecordIdWithKey(key)
        if (!recordId) {
            return null
        }
        def record = (ODocument) orientDbSession().nativeInterface.getRecord(recordId)
        return unmarshallEntity(persistentEntity, record)
    }

    @Override
    protected Serializable persistEntity(PersistentEntity pe, Object obj) {
        println OrientEntity.isAssignableFrom(obj.class)
        def orientEntity = (OrientPersistentEntity) pe
        ProxyFactory proxyFactory = getProxyFactory();
        // if called internally, obj can potentially be a proxy, which won't work.
        obj = proxyFactory.unwrap(obj);
        OIdentifiable nativeEntry = (OIdentifiable) obj['dbInstance']
        if (nativeEntry == null) {
            nativeEntry = OrientGormHelper.createNewOrientEntry(orientEntity, obj, orientDbSession())
        }
        OrientGormHelper.saveEntry(marshallEntity(orientEntity, obj))
        orientDbSession().createEntityAccess(orientEntity, obj).setIdentifierNoConversion(nativeEntry.identity)
        nativeEntry.record
    }

    @Override
    protected void deleteEntity(PersistentEntity pe, Object obj) {
        def orientEntity = (OrientPersistentEntity) pe
        def identity = orientDbSession().createEntityAccess(orientEntity, obj).getIdentifier()
        orientDbSession().nativeInterface.delete(createRecordIdWithKey(identity))
    }

    @Override
    protected void deleteEntities(PersistentEntity pe,
                                  @SuppressWarnings("rawtypes") Iterable objects) {
        def orientEntity = (OrientPersistentEntity) pe
        for (object in objects) {
            def identity = orientDbSession().createEntityAccess(pe, object).getIdentifier()
            orientDbSession().nativeInterface.delete(createRecordIdWithKey(identity))
        }
    }

    @Override
    Serializable refresh(Object o) {
        println "refresh called"
        return null
    }

    protected ORecordId createRecordIdWithKey(Object key) {
        ORecordId recId = null;

        if (key instanceof ORecordId) {
            recId = (ORecordId) key;
        } else if (key instanceof String) {
            recId = new ORecordId((String) key);
        }
        return recId;
    }

    OrientDbSession orientDbSession() {
        (OrientDbSession) session
    }

    @Override
    OrientPersistentEntity getPersistentEntity() {
        return (OrientPersistentEntity) super.getPersistentEntity()
    }

    OIdentifiable marshallEntity(OrientPersistentEntity entity, Object object) {
        OIdentifiable nativeObject = (OIdentifiable) object['dbInstance']
        def access = createEntityAccess(entity, object)
        for (property in entity.getPersistentProperties()) {
            if (property instanceof Simple) {
                PersistentPropertyConverter.get(Simple).marshall(nativeObject, property, access, null)
            }
        }
        for (association in entity.associations) {
            def propertyValue = access.getProperty(association.name)
            def nativeName = entity.getNativePropertyName(association.name)
            if (proxyFactory.unwrap(propertyValue) != null) {
                def associatedEntity = association.getAssociatedEntity()
                def associationAccess = orientDbSession().createEntityAccess(associatedEntity, propertyValue)
                if (association instanceof ToOne) {
                    PersistentPropertyConverter.get(ToOne).marshall(nativeObject, (ToOne) association, access, orientDbSession())
                } else if (association instanceof OneToMany) {
                    handleOneToManyMarshall((OneToMany) association, access, associationAccess)
                }
            }
        }
        nativeObject
    }

    void handleOneToManyMarshall(OneToMany association, EntityAccess entityAccess, EntityAccess associationAccess) {
        if (!association.owningSide && association.referencedPropertyName == null) {
            def list = session.persist((Iterable) associationAccess.getEntity())
            OrientGormHelper.setValue((OrientPersistentEntity) entityAccess.persistentEntity, association, (OIdentifiable) entityAccess.entity['dbInstance'], list)
            return
        }
        if (!association.owningSide && association.referencedPropertyName != null) {
            session.persist((Iterable) associationAccess.getEntity())
        }
    }


    void handleToOneMarshall(ToOne association, EntityAccess entityObject, EntityAccess associationObject) {
        Persister persister = session.getPersister(associationObject.entity);
        def entity = entityObject.getEntity()
        if (association.foreignKeyInChild) {
            persister.persist(associationObject.entity)
        } else {
            def nativeEntry = associationObject.entity['dbInstance']
            if (nativeEntry == null) {
                persister.persist(associationObject.getEntity())
            }
            OrientGormHelper.setValue((OrientPersistentEntity) entityObject.persistentEntity, association, (OIdentifiable) entity['dbInstance'], ((OIdentifiable) associationObject.entity['dbInstance']));
            if (association.referencedPropertyName != null) {
                def value = associationObject.getProperty(association.referencedPropertyName)
                if (value instanceof Collection) {
                    value.add(entity)
                }
            }
        }
    }

    void handleToOneUnmarshall(ToOne association, EntityAccess entityAccess) {
        def entity = entityAccess.entity
        if (association.owningSide && association.foreignKeyInChild) {
            def queryExecutor = new OrientAssociationQueryExecutor((OIdentifiable) entityAccess.identifier, association, session)
            def result = new OrientPersistentSet((Serializable) entityAccess.identifier, orientDbSession(), queryExecutor)
            entityAccess.setProperty(association.name, result);
            return;
        }
        def value = OrientGormHelper.getValue((OrientPersistentEntity) entityAccess.persistentEntity, association, (OIdentifiable) entity['dbInstance'])

        if (value instanceof ORecordId && association.mapping.mappedForm.lazy) {
            println "lazy record id found"
        } else if (value instanceof ORecordId) {
            def cached = orientDbSession().getCachedEntry(association.associatedEntity, ((ORecordId) value).identity)
            if (cached == null) {
                ((ODocument) ((ORecord) value).record).setLazyLoad(false)
                cached = unmarshallEntity((OrientPersistentEntity) association.associatedEntity, value)
            }
            entityAccess.setPropertyNoConversion(association.name, cached)
        } else {
            println "something else here $value"
        }
    }

    void handleOneToManyUnmarshall(OneToMany association, EntityAccess entityAccess) {
        def entity = entityAccess.entity
        if (!association.owningSide && association.referencedPropertyName == null) {
            entityAccess.setProperty(association.name, new OrientLinkedSet(entityAccess, orientDbSession(), association, (OIdentifiable) entity['dbInstance']))
            return;
        }
        if (!association.owningSide && association.referencedPropertyName != null) {
            def queryExecutor = (AssociationQueryExecutor) new OrientAssociationQueryExecutor((OIdentifiable) entityAccess.identifier, association, session)
            def associationSet = new OrientPersistentSet((Serializable) entityAccess.identifier, orientDbSession(), queryExecutor)
            entityAccess.setPropertyNoConversion(association.name, associationSet)
        }
    }

    Object unmarshallEntity(OrientPersistentEntity entity, OIdentifiable nativeEntry) {
        def orientEntity = (OrientPersistentEntity) entity
        if (nativeEntry == null) return nativeEntry;
        if (OrientGormHelper.getOrientClassName(nativeEntry) != null) {
            EntityAccess entityAccess = createEntityAccess(entity, entity.newInstance());
            PersistentPropertyConverter.get(Identity).unmarshall(nativeEntry, entity.identity, entityAccess, orientDbSession())
            orientDbSession().cacheEntry(entity, nativeEntry.identity, entityAccess.entity)
            final Object instance = entityAccess.getEntity();
            instance['dbInstance'] = nativeEntry
            for (property in entityAccess.persistentEntity.getPersistentProperties()) {
                if (property instanceof Simple) {
                    PersistentPropertyConverter.get(Simple).unmarshall(nativeEntry, property, entityAccess, orientDbSession())
                } else if (property instanceof Association) {
                    if (property instanceof ToOne) {
                        PersistentPropertyConverter.get(ToOne).unmarshall(nativeEntry, (ToOne) property, entityAccess, orientDbSession())
                    }
                    if (property instanceof OneToMany) {
                        handleOneToManyUnmarshall((OneToMany) property, entityAccess)
                    }
                }
            }
            firePostLoadEvent(entityAccess.getPersistentEntity(), entityAccess);
            return instance
        }
        return null
    }

    Object unmarshallFromGraph(OrientPersistentEntity entity, OrientElement element) {
        throw new IllegalAccessException("Not yet implemented")
    }

    @Override
    Query createQuery() {
        new OrientQuery(session, persistentEntity)
    }
}
