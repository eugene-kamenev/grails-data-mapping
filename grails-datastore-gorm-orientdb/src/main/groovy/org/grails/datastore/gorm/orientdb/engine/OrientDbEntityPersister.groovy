package org.grails.datastore.gorm.orientdb.engine

import com.orientechnologies.orient.core.db.record.OIdentifiable
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.record.impl.ODocument
import com.tinkerpop.blueprints.impls.orient.OrientElement
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.orientdb.OrientDbPersistentEntity
import org.grails.datastore.gorm.orientdb.OrientDbSession
import org.grails.datastore.gorm.orientdb.extensions.OrientDbGormHelper
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.engine.EntityPersister
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.Simple
import org.grails.datastore.mapping.model.types.ToOne
import org.grails.datastore.mapping.query.Query
import org.springframework.context.ApplicationEventPublisher

@CompileStatic
class OrientDbEntityPersister extends EntityPersister {

    public OrientDbEntityPersister(MappingContext mappingContext, PersistentEntity entity, Session session, ApplicationEventPublisher publisher) {
        super(mappingContext, entity, session, publisher);
    }



    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity pe, Serializable[] keys) {
        def orientEntity = (OrientDbPersistentEntity) pe
        retrieveAllEntities(pe, keys as List<Serializable>)
    }

    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity pe, Iterable<Serializable> keys) {
        def orientEntity = (OrientDbPersistentEntity) pe
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
        def orientEntity = (OrientDbPersistentEntity) pe
        def collection = []
        for (object in objs) {
            this.persistEntity(pe, object)
        }
        return collection
    }

    @Override
    protected Object retrieveEntity(PersistentEntity pe, Serializable key) {
        def orientEntity = (OrientDbPersistentEntity) pe
        def recordId = createRecordIdWithKey(key)
        if (!recordId) {
            return null
        }
        def record = (ODocument) orientDbSession().nativeInterface.getRecord(recordId)
        return unmarshallEntity(persistentEntity, record)
    }

    @Override
    protected Serializable persistEntity(PersistentEntity pe, Object obj) {
        def orientEntity = (OrientDbPersistentEntity) pe
        OIdentifiable nativeEntry = (OIdentifiable) obj['dbInstance']
        if (nativeEntry == null) {
            nativeEntry = OrientDbGormHelper.createNewOrientEntry(orientEntity, obj, orientDbSession())
        }
        def identity = OrientDbGormHelper.saveEntry(marshallEntity(orientEntity, obj)).identity
        orientDbSession().createEntityAccess(orientEntity, obj).setIdentifierNoConversion(identity)
        identity
    }

    @Override
    protected void deleteEntity(PersistentEntity pe, Object obj) {
        def orientEntity = (OrientDbPersistentEntity) pe
        def identity = orientDbSession().createEntityAccess(orientEntity, obj).getIdentifier()
        orientDbSession().nativeInterface.delete(createRecordIdWithKey(identity))
    }

    @Override
    protected void deleteEntities(PersistentEntity pe,
                                  @SuppressWarnings("rawtypes") Iterable objects) {
        def orientEntity = (OrientDbPersistentEntity) pe
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
    OrientDbPersistentEntity getPersistentEntity() {
        return (OrientDbPersistentEntity) super.getPersistentEntity()
    }

    OIdentifiable marshallEntity(OrientDbPersistentEntity entity, Object object) {
        OIdentifiable nativeObject = (OIdentifiable) object['dbInstance']
        def access = createEntityAccess(entity, object)
        for (property in entity.getPersistentProperties()) {
            def nativeName = entity.getNativePropertyName(property.name)
            if (property instanceof Simple) {
                OrientDbGormHelper.setValue(entity, (PersistentProperty) property, nativeObject, access.getProperty(property.name))
            }
        }
        for (association in entity.associations) {
            println "association found $association"
            def propertyValue = access.getProperty(association.name)
            def nativeName = entity.getNativePropertyName(association.name)
            if (propertyValue) {
                def associatedEntity = association.getAssociatedEntity()
                if (association instanceof ToOne) {
                    def associationAccess = orientDbSession().createEntityAccess(associatedEntity, propertyValue)
                    handleToOneMarshall((ToOne)association, access, associationAccess)
                }
            }
        }
        nativeObject
    }



    void handleToOneMarshall(ToOne association, EntityAccess entityObject, EntityAccess associationObject) {
        def entity = entityObject.getEntity()
        if (association.foreignKeyInChild) {
            this.persistEntity(association.associatedEntity, associationObject.entity)
        } else {
            def nativeEntry = associationObject.entity['dbInstance']
            if (nativeEntry == null) {
                this.persistEntity(association.associatedEntity, associationObject.getEntity())
            }
            OrientDbGormHelper.setValue((OrientDbPersistentEntity) entityObject.persistentEntity, association, (OIdentifiable) entity['dbInstance'], ((OIdentifiable) associationObject.entity['dbInstance']));
        }
    }

    void handleToOneUnmarshall(ToOne association, EntityAccess entityAccess) {
        def entity = entityAccess.entity
        if (association.owningSide && association.foreignKeyInChild) {
            def queryExecutor = new OrientDbAssociationQueryExecutor((OIdentifiable) entityAccess.identifier, association, session)
            def result = queryExecutor.query((OIdentifiable) entityAccess.identifier)[0]
            entityAccess.setPropertyNoConversion(association.name, result);
            return;
        }
        def value = OrientDbGormHelper.getValue((OrientDbPersistentEntity) entityAccess.persistentEntity, association, (OIdentifiable) entity['dbInstance'])

        if (value instanceof ORecordId && association.mapping.mappedForm.lazy) {
            println "lazy record id found"
        } else if (value instanceof ODocument) {
            def cached = orientDbSession().getCachedEntry(association.associatedEntity, ((ODocument)value).identity)
            if (cached == null) {
                cached = unmarshallEntity((OrientDbPersistentEntity) association.associatedEntity, value)
            }
            entityAccess.setPropertyNoConversion(association.name, cached)
        } else {
            println "something else here"
        }
    }

    Object unmarshallEntity(OrientDbPersistentEntity entity, OIdentifiable nativeEntry) {
        def orientEntity = (OrientDbPersistentEntity) entity
        if (nativeEntry == null) return nativeEntry;
        if (OrientDbGormHelper.getOrientClassName(nativeEntry) != null) {
            EntityAccess entityAccess = createEntityAccess(entity, entity.newInstance());
            entityAccess.setIdentifierNoConversion(nativeEntry.identity);
            orientDbSession().cacheEntry(entity, nativeEntry.identity, entityAccess.entity)
            final Object instance = entityAccess.getEntity();
            instance['dbInstance'] = nativeEntry
            for (property in entityAccess.persistentEntity.getPersistentProperties()) {
                if (property instanceof Simple) {
                    entityAccess.setProperty(property.name, OrientDbGormHelper.getValue(entity, property, nativeEntry))
                } else if (property instanceof Association) {
                    if (property instanceof ToOne) {
                        handleToOneUnmarshall((ToOne)property, entityAccess)
                    }

                    // if (document.containsField(nativeName)) {
                    // entityAccess.setPropertyNoConversion(property.name, unmarshallEntity((OrientDbPersistentEntity) association.associatedEntity, (ODocument) document.field(nativeName)));
                    //}
                }
            }
            firePostLoadEvent(entityAccess.getPersistentEntity(), entityAccess);
            return instance
        }
        return null
    }

    Object unmarshallFromGraph(OrientDbPersistentEntity entity, OrientElement element) {
        throw new IllegalAccessException("Not yet implemented")
    }

    @Override
    Query createQuery() {
        new OrientDbQuery(session, persistentEntity)
    }
}
