package org.grails.datastore.gorm.orient.engine

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.orient.OrientDbSession
import org.grails.datastore.gorm.orient.OrientPersistentEntity
import org.grails.datastore.gorm.orient.extensions.OrientGormHelper
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.engine.EntityPersister
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.query.Query
import org.springframework.context.ApplicationEventPublisher

/**
 *
 */
@CompileStatic
class OrientDbEntityPersister extends EntityPersister {


    OrientDbEntityPersister(MappingContext mappingContext, PersistentEntity entity, Session session, ApplicationEventPublisher publisher) {
        super(mappingContext, entity, session, publisher)
    }

    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity pe, Serializable[] keys) {
        def resultList = []
        def recordIdsList = []
        def containsInvalidKeys = OrientGormHelper.checkForRecordIds(keys.toList(), [])
        if (!containsInvalidKeys) {
            def query = createQuery().list()
        }
        return resultList
    }

    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity pe, Iterable<Serializable> keys) {
        return null
    }

    @Override
    protected List<Serializable> persistEntities(PersistentEntity pe, @SuppressWarnings("rawtypes") Iterable objs) {
        return null
    }

    @Override
    protected Object retrieveEntity(PersistentEntity pe, Serializable key) {
        return null
    }

    @Override
    protected Serializable persistEntity(PersistentEntity pe, Object obj) {
        return null
    }

    @Override
    protected void deleteEntity(PersistentEntity pe, Object obj) {

    }

    @Override
    protected void deleteEntities(PersistentEntity pe,
                                  @SuppressWarnings("rawtypes") Iterable objects) {

    }

    @Override
    Serializable refresh(Object o) {
        return null
    }

    OrientDbSession orientDbSession() {
        (OrientDbSession) session
    }

    @Override
    OrientPersistentEntity getPersistentEntity() {
        return (OrientPersistentEntity) super.getPersistentEntity()
    }

    @Override
    Query createQuery() {
        new OrientQuery(session, persistentEntity)
    }
}
