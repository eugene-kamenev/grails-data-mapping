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
import org.grails.datastore.gorm.orientdb.mapping.config.OrientAttribute
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.core.SessionImplementor
import org.grails.datastore.mapping.engine.AssociationIndexer
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.engine.NativeEntryEntityPersister
import org.grails.datastore.mapping.engine.PropertyValueIndexer
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.PropertyMapping
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.Identity
import org.grails.datastore.mapping.model.types.Simple
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
            if (value == null && getValue(document, name) == null) return;
            document.field(name, value);
        }
    };

    public static
    final ValueRetrievalStrategy VERTEX_EDGE_VALUE_RETRIVAL_STRATEGY = new ValueRetrievalStrategy() {
        @Override
        Object getValue(Object orientVertex, String name) {
            if (orientVertex instanceof ODocument) {
                return orientVertex.field(name);
            }
            if (orientVertex instanceof OrientElement) {
                return orientVertex.getProperty(name)
            }
            return null
        }

        @Override
        void setValue(Object orientVertex, String name, Object value) {
            if (orientVertex instanceof ODocument) {
                if (value == null && getValue(orientVertex, name) == null) return;
                orientVertex.field(name, value);
            }
            if (orientVertex instanceof OrientElement) {
                if (value == null && getValue(orientVertex, name) == null) return;
                orientVertex.setProperty(name, value)
            }
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

    public Object createObjectFromNativeEntry(PersistentEntity persistentEntity, Serializable nativeKey, OIdentifiable nativeEntry) {
        persistentEntity = discriminatePersistentEntity(persistentEntity, nativeEntry);

        cacheNativeEntry(persistentEntity, nativeKey, nativeEntry);

        Object obj = newEntityInstance(persistentEntity);
        refreshObjectStateFromNativeEntry(persistentEntity, obj, nativeKey, nativeEntry, false);
        return obj;
    }

    Object unmarshallDocument(OrientDbPersistentEntity entity, ODocument document) {
        if (document == null) {
            return document
        }
        if (document.className != null) {
            EntityAccess entityAccess = orientDbSession().createEntityAccess(entity, entity.newInstance());
            entityAccess.setIdentifierNoConversion(document.identity);
            final Object instance = entityAccess.getEntity();
            // orientDbSession().cacheInstance(persistentEntity.getJavaClass(), document.identity, entity);
            for (property in entityAccess.persistentEntity.getPersistentProperties()) {
                String nativeName = entity.getNativePropertyName(property.name)
                if (property instanceof Simple) {
                    if (document.containsField(nativeName)) {
                        entityAccess.setProperty(property.name, document.field(nativeName));
                    }
                } else if (property instanceof Association) {
                    def association = (Association) property
                    if (document.containsField(nativeName)) {
                        entityAccess.setPropertyNoConversion(property.name, unmarshallDocument((OrientDbPersistentEntity) association.associatedEntity, (ODocument) document.field(nativeName)));
                    }
                }
            }
            return instance
        }
        return null
    }

    Object unmarshallFromGraph(OrientDbPersistentEntity entity, OrientElement element) {
        throw new IllegalAccessException("Not yet implemented")
    }

    @Override
    protected Serializable convertIdIfNecessary(PersistentEntity entity, Serializable nativeKey) {
        if (nativeKey instanceof ODocument) {
            return nativeKey.identity
        }
        if (nativeKey instanceof ORecordId) {
            return nativeKey;
        }
        return super.convertIdIfNecessary(entity, nativeKey)
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
            return entry.identity.getClusterPosition()
        }
        return null
    }

    @Override
    PropertyValueIndexer getPropertyIndexer(PersistentProperty property) {
        return null
    }

    @Override
    AssociationIndexer getAssociationIndexer(OIdentifiable nativeEntry, Association association) {
        return new OrientDbAssociationIndexer(nativeEntry, association, orientDbSession())
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
            // TODO: decide what to do here return orientDbSession().graph.createEdge
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
        def recordId = createRecordIdWithKey(key)
        if (recordId) {
            if (orientEntity.document) {
                return orientDbSession().documentTx.load(recordId)
            }
            if (orientEntity.vertex) {
                return orientDbSession().graph.getVertex(recordId)
            }
            if (orientEntity.edge) {
                return orientDbSession().graph.getEdge(recordId)
            }
        }
        return null
    }

    @Override
    protected Object storeEntry(PersistentEntity persistentEntity, EntityAccess entityAccess, Object storeId, OIdentifiable nativeEntry) {
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
        if (orientEntity.graph && entry instanceof OrientElement) {
            def vertex = (OrientElement) entry
            vertex.save()
            return;
        }
        def doc = (ODocument) entry
        doc.className = orientEntity.className
        orientDbSession().documentTx.save(doc)
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
     * @param < T >   The native type
     */
    static interface ValueRetrievalStrategy<T> {
        Object getValue(T t, String name);

        void setValue(T t, String name, Object value);
    }

    protected class OrientDbAssociationIndexer implements AssociationIndexer {
        private OIdentifiable nativeEntry;
        private Association association;
        private OrientDbSession session;
        private boolean isReference = true;

        public OrientDbAssociationIndexer(OIdentifiable nativeEntry, Association association, OrientDbSession session) {
            this.nativeEntry = nativeEntry;
            this.association = association;
            this.session = session;
            this.isReference = this.isReference(association);
        }

        protected boolean isReference(Association association) {
            PropertyMapping mapping = association.getMapping();
            if (mapping != null) {
                OrientAttribute attribute = (OrientAttribute) mapping.getMappedForm();
                if (attribute != null) {
                    // return attribute.isReference();
                }
            }
            return true;
        }

        @Override
        public boolean doesReturnKeys() {
            return true;
        }

        public void preIndex(final Object primaryKey, final List foreignKeys) {
            // if the association is a unidirectional one-to-many we store the keys
            // embedded in the owning entity, otherwise we use a foreign key
            if (!association.isBidirectional()) {
                List dbRefs = new ArrayList();
                for (Object foreignKey : foreignKeys) {
                    dbRefs.add(createRecordIdWithKey(foreignKey));
                }
                // update the native entry directly.
                getValueRetrievalStrategy().setValue(nativeEntry, association.name, dbRefs);
            }
        }

        public void index(final Object primaryKey, final List foreignKeys) {
            // indexing is handled by putting the data in the native entry before it is persisted, see preIndex above.
        }

        public List query(Object primaryKey) {
            // for a unidirectional one-to-many we use the embedded keys
            if (!association.isBidirectional()) {
                final Object indexed = getValueRetrievalStrategy().getValue(nativeEntry, association.getName());
                if (!(indexed instanceof Collection)) {
                    return Collections.emptyList();
                }
                List indexedList = getIndexedAssociationsAsList(indexed);

                if (associationsAreDbRefs(indexedList)) {
                    return extractIdsFromDbRefs(indexedList);
                }
                return indexedList;
            }
            // for a bidirectional one-to-many we use the foreign key to query the inverse side of the association
            Association inverseSide = association.getInverseSide();
            Query query = session.createQuery(association.getAssociatedEntity().getJavaClass());
            query.eq(inverseSide.getName(), primaryKey);
            query.projections().id();
            return query.list();
        }

        public PersistentEntity getIndexedEntity() {
            return association.getAssociatedEntity();
        }

        public void index(Object primaryKey, Object foreignKey) {
            // TODO: Implement indexing of individual entities
        }

        private List getIndexedAssociationsAsList(Object indexed) {
            return (indexed instanceof List) ? (List) indexed : new ArrayList(((Collection) indexed));
        }

        private boolean associationsAreDbRefs(List indexedList) {
            return !indexedList.isEmpty() && (indexedList.get(0) instanceof ORID);
        }

        private List extractIdsFromDbRefs(List indexedList) {
            List resolvedDbRefs = new ArrayList();
            for (Object indexedAssociation : indexedList) {
                resolvedDbRefs.add(((ORecordId) indexedAssociation).identity);
            }
            return resolvedDbRefs;
        }
    }
}
