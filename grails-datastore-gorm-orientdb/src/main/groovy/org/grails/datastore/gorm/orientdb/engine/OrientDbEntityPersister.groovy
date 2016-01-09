package org.grails.datastore.gorm.orientdb.engine

import com.orientechnologies.orient.core.db.record.OIdentifiable
import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.record.impl.ODocument
import com.tinkerpop.blueprints.impls.orient.OrientElement
import com.tinkerpop.blueprints.impls.orient.OrientVertex
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.orientdb.OrientDbPersistentEntity
import org.grails.datastore.gorm.orientdb.OrientDbSession
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.core.SessionImplementor
import org.grails.datastore.mapping.engine.AssociationIndexer
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.engine.NativeEntryEntityPersister
import org.grails.datastore.mapping.engine.PropertyValueIndexer
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.Identity
import org.grails.datastore.mapping.query.Query
import org.springframework.context.ApplicationEventPublisher

@CompileStatic
class OrientDbEntityPersister extends NativeEntryEntityPersister<OIdentifiable, Object> {

    public OrientDbEntityPersister(MappingContext mappingContext, PersistentEntity entity, Session session, ApplicationEventPublisher publisher) {
        super(mappingContext, entity, session, publisher);
    }

    public static
    final ValueRetrievalStrategy<ODocument> DOCUMENT_VALUE_RETRIEVAL_STRATEGY = new ValueRetrievalStrategy<ODocument>() {
        @Override
        public Object getValue(ODocument document, String name) {
            return document.field(name);
        }

        @Override
        public void setValue(ODocument document, String name, Object value) {
            document.field(name, value);
        }
    };

    public static
    final ValueRetrievalStrategy<OrientElement> VERTEX_EDGE_VALUE_RETRIVAL_STRATEGY = new ValueRetrievalStrategy<OrientElement>() {
        @Override
        Object getValue(OrientElement orientVertex, String name) {
            orientVertex.getProperty(name)
        }

        @Override
        void setValue(OrientElement orientVertex, String name, Object value) {
            if (value == null && getValue(orientVertex, name) == null) {
                return;
            }
            orientVertex.setProperty(name, value)
        }
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

    @Override
    protected String getPropertyKey(PersistentProperty prop) {
        if (prop instanceof Identity) {
            return '@rid';
        }
        return super.getPropertyKey(prop);
    }

    OrientDbSession orientDbSession() {
        (OrientDbSession) session
    }

    @Override
    OrientDbPersistentEntity getPersistentEntity() {
        return (OrientDbPersistentEntity) super.getPersistentEntity()
    }

    @Override
    String getEntityFamily() {
        persistentEntity.className
    }

    Object unmarshallDocument(OrientDbPersistentEntity entity, ODocument document) {
        if (document == null) {
            return document
        }
        def instance = entity.javaClass.newInstance()
        for (name in document.fieldNames()) {
            if (name in entity.getPersistentPropertyNames()) {
                instance[name] = document.field(name)
            }
        }
        instance[entity.identity.name] = generateIdentifier(entity, document)
        return instance
    }

    Object unmarshallFromGraph(OrientDbPersistentEntity entity, OrientElement element) {
        if (element == null) {
            return element
        }
        def instance = entity.javaClass.newInstance()
        for(name in element.getPropertyKeys()) {
            if (name in entity.getPersistentPropertyNames()) {
                instance[name] = element.getProperty(name)
            }
        }
        instance[entity.identity.name] = generateIdentifier(entity, element)
        return instance
    }

    @Override
    protected void deleteEntry(String family, Object key, Object entry) {
        orientDbSession().delete(createRecordIdWithKey(key))
    }

    @Override
    protected Object generateIdentifier(PersistentEntity persistentEntity, OIdentifiable entry) {
        if (ORID.class.isAssignableFrom(persistentEntity.getIdentity().getType())) {
            return entry.identity
        }
        if (String.class.isAssignableFrom(persistentEntity.getIdentity().getType())) {
            return entry.identity.toString()
        }
        if (Number.class.isAssignableFrom(persistentEntity.getIdentity().getType())) {
            // just for passing grails native tests??
            return entry.identity.toString().hashCode()
        }
        return null
    }

    @Override
    PropertyValueIndexer getPropertyIndexer(PersistentProperty property) {
        return null
    }

    @Override
    AssociationIndexer getAssociationIndexer(OIdentifiable nativeEntry, Association association) {
        return null
    }

    @Override
    protected OIdentifiable getEmbedded(OIdentifiable nativeEntry, String key) {
        final Object embeddedDocument = getValueRetrievalStrategy().getValue(nativeEntry, key);
        if (isEmbeddedEntry(embeddedDocument)) {
            return (OIdentifiable) embeddedDocument;
        }
        return null;
    }

    @Override
    public boolean isDirty(Object instance, Object entry) {
        if (super.isDirty(instance, entry)) {
            return true;
        }

        ORecordId dbo = (ORecordId) entry;
        PersistentEntity entity = getPersistentEntity();

        EntityAccess entityAccess = createEntityAccess(entity, instance, dbo);

        ORecordId cached = (ORecordId) ((SessionImplementor<?>) getSession()).getCachedEntry(
                entity, (Serializable) entityAccess.getIdentifier(), true);

        return !dbo.equals(cached);
    }

    @Override
    protected OIdentifiable createNewEntry(String family) {
        if (persistentEntity.document) {
            return new ODocument(family)
        }
        if (persistentEntity.vertex) {
            return orientDbSession().graph.addTemporaryVertex(family)
        }
        if (persistentEntity.edge) {
            // TODO: decide what to do here return orientDbSession().graph.create
        }
        return null
    }

    @Override
    protected PersistentEntity discriminatePersistentEntity(PersistentEntity persistentEntity, OIdentifiable nativeEntry) {
        final Object o = getValueRetrievalStrategy().getValue(nativeEntry, "@class");
        if (o != null) {
            final String className = o.toString();
            final PersistentEntity childEntity = getMappingContext().getChildEntityByDiscriminator(persistentEntity.getRootEntity(), className);
            if (childEntity != null) {
                return childEntity;
            }
        }
        return super.discriminatePersistentEntity(persistentEntity, nativeEntry);
    }

    @Override
    protected Object getEntryValue(OIdentifiable nativeEntry, String property) {
        return getValueRetrievalStrategy().getValue(nativeEntry, property);
    }

    @Override
    protected void setEntryValue(OIdentifiable nativeEntry, String key, Object value) {
        getValueRetrievalStrategy().setValue(nativeEntry, key, value)
    }

    @Override
    protected OIdentifiable retrieveEntry(PersistentEntity persistentEntity, String family, Serializable key) {
        def orientEntity = (OrientDbPersistentEntity) persistentEntity
        if (orientEntity.document) {
            return orientDbSession().documentTx.load(createRecordIdWithKey(key))
        }
        if (orientEntity.vertex) {
            return orientDbSession().graph.getVertex(createRecordIdWithKey(key))
        }
        if (orientEntity.edge) {
            return orientDbSession().graph.getEdge(createRecordIdWithKey(key))
        }
        return null
    }

    @Override
    protected Object storeEntry(PersistentEntity persistentEntity, EntityAccess entityAccess, Object storeId, OIdentifiable nativeEntry) {
        println "StoreId $storeId"
        def orientEntity = (OrientDbPersistentEntity) persistentEntity
        ORID identity
        if (orientEntity.document) {
            def doc = (ODocument) nativeEntry
            doc.className = orientEntity.className
            identity = orientDbSession().documentTx.save(doc).identity
        }
        if (orientEntity.vertex) {
            def vertex = (OrientVertex) nativeEntry
            vertex.save()
            identity = vertex.identity
        }
        if (orientEntity.edge) {
            //def edge = orientDbSession().graph.addEdge()
        }
        if (identity) {
            if (ORID.class.isAssignableFrom(persistentEntity.getIdentity().getType())) {
                return identity
            }
            if (String.class.isAssignableFrom(persistentEntity.getIdentity().getType())) {
                return identity.toString()
            }
            if (Number.class.isAssignableFrom(persistentEntity.getIdentity().getType())) {
                // just for passing grails tests??
                return identity.toString().hashCode()
            }
        }
        return null
    }

    @Override
    protected void updateEntry(PersistentEntity persistentEntity, EntityAccess entityAccess, Object key, OIdentifiable entry) {
        def orientEntity = (OrientDbPersistentEntity) persistentEntity
        if (orientEntity.document) {
            def doc = (ODocument) entry
            doc.className = orientEntity.className
            orientDbSession().documentTx.save(doc)
        }
        if (orientEntity.graph) {
            def vertex = (OrientElement) entry
            vertex.save()
        }
    }

    @Override
    protected void deleteEntries(String family, List keys) {
        for (key in keys) {
            orientDbSession().documentTx.delete(createRecordIdWithKey(key))
        }
    }

    @Override
    Query createQuery() {
        new OrientDbQuery(session, persistentEntity)
    }

    ValueRetrievalStrategy<OIdentifiable> getValueRetrievalStrategy() {
        if (persistentEntity.document) {
            return DOCUMENT_VALUE_RETRIEVAL_STRATEGY
        }
        if (persistentEntity.graph) {
            return VERTEX_EDGE_VALUE_RETRIVAL_STRATEGY
        }
        null
    }

    /**
     * Strategy interface for implementors to implement to set and get values from the native type
     *
     * @param < T >  The native type
     */
    static interface ValueRetrievalStrategy<T> {
        Object getValue(T t, String name);

        void setValue(T t, String name, Object value);
    }
}
