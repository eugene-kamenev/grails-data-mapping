package org.grails.datastore.gorm.orientdb.engine

import com.orientechnologies.orient.core.db.record.OIdentifiable
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.orientdb.OrientDbPersistentEntity
import org.grails.datastore.gorm.orientdb.OrientDbSession
import org.grails.datastore.gorm.orientdb.collection.OrientDbResultList
import org.grails.datastore.gorm.orientdb.extensions.OrientDbGormHelper
import org.grails.datastore.mapping.engine.AssociationQueryExecutor
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.types.Association

/**
 * OrientDB lazy association executor, need to be tested in remote mode with lazy-loading disabled
 */
@CompileStatic
class OrientDbLinkedAssociationExecutor implements AssociationQueryExecutor<OIdentifiable, Object> {
    Association lazyAssociation
    OrientDbSession session

    OrientDbLinkedAssociationExecutor(Association lazyAssociation, OrientDbSession session) {
        this.lazyAssociation = lazyAssociation
        this.session = session
    }

    @Override
    List<Object> query(OIdentifiable primaryKey) {
        def value = OrientDbGormHelper.getValue((OrientDbPersistentEntity) lazyAssociation.owner, lazyAssociation, primaryKey.record)
        if (value == null) value = [null]
        if (!(value instanceof Collection)) {
            value = [value]
        }
        return new OrientDbResultList(0, value.iterator(), (OrientDbEntityPersister) session.getPersister(indexedEntity)).toList()
    }

    @Override
    PersistentEntity getIndexedEntity() {
        return lazyAssociation.associatedEntity
    }

    @Override
    boolean doesReturnKeys() {
        return false
    }
}
