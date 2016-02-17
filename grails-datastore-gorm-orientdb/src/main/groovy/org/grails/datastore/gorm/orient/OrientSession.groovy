package org.grails.datastore.gorm.orient

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap
import com.googlecode.concurrentlinkedhashmap.EvictionListener
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.db.record.OIdentifiable
import com.tinkerpop.blueprints.impls.orient.OrientGraph
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.orient.engine.OrientEntityPersister
import org.grails.datastore.gorm.orient.engine.OrientPendingRelationshipDelete
import org.grails.datastore.gorm.orient.engine.OrientPendingRelationshipInsert
import org.grails.datastore.gorm.orient.engine.RelationshipUpdateKey
import org.grails.datastore.gorm.orient.extensions.OrientGormHelper
import org.grails.datastore.mapping.core.AbstractSession
import org.grails.datastore.mapping.core.impl.PendingDelete
import org.grails.datastore.mapping.core.impl.PendingInsert
import org.grails.datastore.mapping.core.impl.PendingOperation
import org.grails.datastore.mapping.core.impl.PendingUpdate
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.engine.Persister
import org.grails.datastore.mapping.engine.types.CustomTypeMarshaller
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.Custom
import org.grails.datastore.mapping.model.types.Simple
import org.grails.datastore.mapping.query.api.QueryableCriteria
import org.grails.datastore.mapping.transactions.Transaction
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DataAccessResourceFailureException

import java.util.concurrent.ConcurrentLinkedQueue
/**
 * Represents OrientDB GORM Session implementation
 *
 * @author eugenekamenev
 */
@CompileStatic
class OrientSession extends AbstractSession<ODatabaseDocumentTx> {

    private static final EvictionListener<RelationshipUpdateKey, Collection<Serializable>> EXCEPTION_THROWING_INSERT_LISTENER =
            new EvictionListener<RelationshipUpdateKey, Collection<Serializable>>() {
                public void onEviction(RelationshipUpdateKey association, Collection<Serializable> value) {
                    throw new DataAccessResourceFailureException("Maximum number (5000) of relationship update operations to flush() exceeded. Flush the session periodically to avoid this error for batch operations.");
                }
            };

    protected Map<RelationshipUpdateKey, Collection<Serializable>> pendingRelationshipInserts =
            new ConcurrentLinkedHashMap.Builder<RelationshipUpdateKey, Collection<Serializable>>()
                    .listener(EXCEPTION_THROWING_INSERT_LISTENER)
                    .maximumWeightedCapacity(5000).build();

    protected Map<RelationshipUpdateKey, Collection<Serializable>> pendingRelationshipDeletes =
            new ConcurrentLinkedHashMap.Builder<RelationshipUpdateKey, Collection<Serializable>>()
                    .listener(EXCEPTION_THROWING_INSERT_LISTENER)
                    .maximumWeightedCapacity(5000).build();

    protected ODatabaseDocumentTx currentDocumentConnection
    protected OrientGraph currentActiveGraph

    OrientSession(OrientDatastore datastore, MappingContext mappingContext, ApplicationEventPublisher publisher, boolean stateless, ODatabaseDocumentTx connection) {
        super(datastore, mappingContext, publisher, stateless)
        currentDocumentConnection = connection
    }

    @Override
    protected Persister createPersister(Class cls, MappingContext mappingContext) {
        final PersistentEntity entity = mappingContext.getPersistentEntity(cls.getName());
        return (entity != null) ? new OrientEntityPersister(mappingContext, entity, this, publisher) : null;
    }

    @Override
    protected Transaction beginTransactionInternal() {
        new OrientTransaction(documentTx)
    }

    /**
     * Adds a relationship that is pending insertion
     *
     * @param association The association
     * @param id The id
     */
    public void addPendingRelationshipInsert(Serializable parentId, Association association, Serializable id) {
        addRelationshipUpdate(parentId, association, id, this.pendingRelationshipInserts);
    }

    /**
     * Adds a relationship that is pending deletion
     *
     * @param association The association
     * @param id The id
     */
    public void addPendingRelationshipDelete(Serializable parentId, Association association, Serializable id) {
        addRelationshipUpdate(parentId, association, id, this.pendingRelationshipDeletes);
    }

    protected void addRelationshipUpdate(Serializable parentId, Association association, Serializable id, Map<RelationshipUpdateKey, Collection<Serializable>> targetMap) {
        final RelationshipUpdateKey key = new RelationshipUpdateKey(OrientGormHelper.createRecordId(parentId), association);
        Collection<Serializable> inserts = targetMap.get(key);
        if (inserts == null) {
            inserts = new ConcurrentLinkedQueue<Serializable>();
            targetMap.put(key, inserts);
        }
        inserts.add(OrientGormHelper.createRecordId(id));
    }

    @Override
    protected void clearPendingOperations() {
        try {
            super.clearPendingOperations();
        } finally {
            pendingRelationshipInserts.clear();
        }
    }

    /**
    * TODO: add implementation
    *
    * @param pendingDeletes
    */
    @Override
    protected void flushPendingUpdates(Map<PersistentEntity, Collection<PendingUpdate>> updates) {
        final Set<PersistentEntity> entities = updates.keySet();
        for (PersistentEntity entity : entities) {
            final Collection<PendingUpdate> pendingUpdates = updates.get(entity);
            OrientPersistentEntity graphPersistentEntity = (OrientPersistentEntity) entity;

            // final boolean isVersioned = entity.hasProperty(GormProperties.VERSION, Long.class) && entity.isVersioned();

            for (PendingUpdate pendingUpdate : pendingUpdates) {
                final List<PendingOperation> preOperations = pendingUpdate.getPreOperations();
                executePendings(preOperations);

                pendingUpdate.run();

                if(pendingUpdate.isVetoed()) continue;

                final EntityAccess access = pendingUpdate.getEntityAccess();
                final List<PendingOperation<Object, Serializable>> cascadingOperations = new ArrayList<PendingOperation<Object, Serializable>>(pendingUpdate.getCascadeOperations());

                def id = OrientGormHelper.createRecordId(pendingUpdate.getNativeKey());
                final Object object = pendingUpdate.getObject();
                final DirtyCheckable dirtyCheckable = (DirtyCheckable) object;
                final List<String> dirtyPropertyNames = dirtyCheckable.listDirtyPropertyNames();
                final List<String> nulls = new ArrayList<String>();
                for (String dirtyPropertyName : dirtyPropertyNames) {
                    final PersistentProperty property = entity.getPropertyByName(dirtyPropertyName);
                    if(property !=null){
                        if (property instanceof Simple) {
                            String name = property.getName();
                            Object value = access.getProperty(name);
                            if (value != null) {
                                // simpleProps.put(name,  mappingContext.convertToNative(value));
                            }
                            else {
                                nulls.add(name);
                            }
                        }
                        else if(property instanceof Custom) {
                            Custom<Map<String,Object>> custom = (Custom<Map<String,Object>>)property;
                            final CustomTypeMarshaller<Object, Map<String, Object>, Map<String, Object>> customTypeMarshaller = custom.getCustomTypeMarshaller();
                            Object value = access.getProperty(property.getName());
                            //customTypeMarshaller.write(custom, value, simpleProps);
                        }
                    }
                }
                processPendingRelationshipUpdates(graphPersistentEntity, access, (Serializable) id, cascadingOperations);
                dirtyCheckable.trackChanges();
                executePendings(cascadingOperations);
                id.record.save()
            }
        }
    }

    /**
     * TODO: add implementation
     *
     * @param pendingDeletes
     */
    @Override
    protected void flushPendingInserts(Map<PersistentEntity, Collection<PendingInsert>> inserts) {
        final Set<PersistentEntity> entities = inserts.keySet();
        List<PendingOperation<Object, Serializable>> cascadingOperations = new ArrayList<PendingOperation<Object, Serializable>>();
        for (PersistentEntity entity : entities) {
            final Collection<PendingInsert> entityInserts = inserts.get(entity);
            for (final PendingInsert entityInsert : entityInserts) {
                if(entityInsert.wasExecuted()) {
                    processPendingRelationshipUpdates(entity, entityInsert.getEntityAccess(), (Serializable) entityInsert.getNativeKey(), cascadingOperations);
                    cascadingOperations.addAll(entityInsert.getCascadeOperations());
                }
            }
        }
        println "processing $cascadingOperations"
        executePendings(cascadingOperations);
    }

    /**
     * TODO: add implementation
     *
     * @param pendingDeletes
     */
    @Override
    protected void flushPendingDeletes(Map<PersistentEntity, Collection<PendingDelete>> pendingDeletes) {
        super.flushPendingDeletes(pendingDeletes)
    }

    private void processPendingRelationshipUpdates(PersistentEntity entity, EntityAccess access, Serializable id, List<PendingOperation<Object, Serializable>> cascadingOperations) {
        for (Association association : entity.getAssociations()) {
            processPendingRelationshipUpdates(access, id, association, cascadingOperations);
        }
    }

    private void processPendingRelationshipUpdates(EntityAccess parent, Serializable parentId, Association association, List<PendingOperation<Object, Serializable>> cascadingOperations) {
        final RelationshipUpdateKey key = new RelationshipUpdateKey(OrientGormHelper.createRecordId(parentId), association);
        final Collection<Serializable> pendingInserts = pendingRelationshipInserts[key]
        println "adding pending Inserts $pendingInserts"
        if(pendingInserts != null) {
            cascadingOperations.add(new OrientPendingRelationshipInsert(parent, association, pendingInserts, this, ((OIdentifiable)parentId).identity.isNew()));
        }
        final Collection<Serializable> pendingDeletes = pendingRelationshipDeletes.get(key);
        if(pendingDeletes != null) {
            cascadingOperations.add(new OrientPendingRelationshipDelete(parent, association, pendingDeletes));
        }
    }

    /**
     * TODO: add implementation
     *
     * @param criteria The criteria
     * @param properties The properties
     * @return
     */
    @Override
    long updateAll(QueryableCriteria criteria, Map<String, Object> properties) {
        return super.updateAll(criteria, properties)
    }

    OrientGraph getGraph() {
        if (currentActiveGraph == null) {
            currentActiveGraph = new OrientGraph(documentTx)
        }
        currentActiveGraph
    }

    @Override
    void flush() {
        super.flush()
        if (transaction.active && !transaction.rollbackOnly) {
            transaction.commit()
        }
    }

    @Override
    void clear() {
        super.clear()
        if (!transaction.active && transaction.rollbackOnly) {
            transaction.rollbackOnly()
        }
    }

    @Override
    void disconnect() {
        if (isConnected()) {
            super.disconnect()
            if (currentActiveGraph != null && !currentActiveGraph.closed) {
                currentActiveGraph.shutdown(false, false)
            }
            if (!documentTx.closed) {
                documentTx.close()
            }
        }
    }

    ODatabaseDocumentTx getDocumentTx() {
        currentDocumentConnection
    }

    @Override
    ODatabaseDocumentTx getNativeInterface() {
        return this.currentDocumentConnection
    }

    @Override
    OrientTransaction getTransaction() {
        (OrientTransaction) super.getTransaction()
    }

    @Override
    OrientDatastore getDatastore() {
        return (OrientDatastore) super.getDatastore()
    }
}
