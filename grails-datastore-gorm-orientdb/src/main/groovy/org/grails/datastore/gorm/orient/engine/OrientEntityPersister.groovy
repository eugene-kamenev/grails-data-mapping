package org.grails.datastore.gorm.orient.engine

import com.orientechnologies.orient.core.db.record.OIdentifiable
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.record.ORecord
import com.tinkerpop.blueprints.impls.orient.OrientElement
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.orient.OrientDbSession
import org.grails.datastore.gorm.orient.OrientPersistentEntity
import org.grails.datastore.gorm.orient.collection.OrientResultList
import org.grails.datastore.gorm.orient.extensions.OrientGormHelper
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.engine.EntityPersister
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.types.Identity
import org.grails.datastore.mapping.proxy.ProxyFactory
import org.grails.datastore.mapping.query.Query
import org.springframework.context.ApplicationEventPublisher

/**
 * OrientDB entity persister implementation
 */
@CompileStatic
@Slf4j
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
        def containsInvalidKeys = OrientGormHelper.checkForRecordIds(keys.toList(), [])
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
        def orientEntity = (OrientPersistentEntity) pe
        ProxyFactory proxyFactory = getProxyFactory();
        obj = proxyFactory.unwrap(obj);
        def entityAccess = orientDbSession().createEntityAccess(pe, obj)
        def entityId = OrientGormHelper.createRecordId(entityAccess.getIdentifier())
        if (entityId == null) {
            entityId = OrientGormHelper.createNewOrientEntry(orientEntity, obj, orientDbSession())
            // we need to save it right now, it should be attached to ODocumentTx
            OrientGormHelper.saveEntry(entityId)
            entityAccess.setIdentifierNoConversion(entityId.identity)
        }
        OrientGormHelper.saveEntry(marshallEntity(orientEntity, entityAccess))
        entityId.record
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

    OrientDbSession orientDbSession() {
        (OrientDbSession) session
    }

    @Override
    OrientPersistentEntity getPersistentEntity() {
        return (OrientPersistentEntity) super.getPersistentEntity()
    }

    OIdentifiable marshallEntity(OrientPersistentEntity entity, EntityAccess entityAccess) {
        OIdentifiable nativeObject = ((OIdentifiable) entityAccess.identifier).record
        for (property in entity.getPersistentProperties()) {
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
                OrientPersistentPropertyConverter.getForPersistentProperty(property).unmarshall(nativeEntry, property, entityAccess, orientDbSession())
            }
            firePostLoadEvent(entityAccess.getPersistentEntity(), entityAccess);
            return instance
        }
        return null
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
