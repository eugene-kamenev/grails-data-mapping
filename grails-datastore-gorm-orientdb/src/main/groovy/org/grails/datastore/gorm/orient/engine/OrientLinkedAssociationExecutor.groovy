package org.grails.datastore.gorm.orient.engine

import com.orientechnologies.orient.core.db.record.OIdentifiable
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.orient.OrientPersistentEntity
import org.grails.datastore.gorm.orient.OrientSession
import org.grails.datastore.gorm.orient.collection.OrientResultList
import org.grails.datastore.gorm.orient.extensions.OrientGormHelper
import org.grails.datastore.mapping.engine.AssociationQueryExecutor
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.types.Association

/**
 * OrientDB lazy association executor, need to be tested in remote mode with lazy-loading disabled
 *
 * @author eugenekamenev
 */
@CompileStatic
class OrientLinkedAssociationExecutor implements AssociationQueryExecutor<OIdentifiable, Object> {
    Association lazyAssociation
    OrientSession session

    OrientLinkedAssociationExecutor(Association lazyAssociation, OrientSession session) {
        this.lazyAssociation = lazyAssociation
        this.session = session
    }

    @Override
    List<Object> query(OIdentifiable primaryKey) {
        def value = OrientGormHelper.getValue((OrientPersistentEntity) lazyAssociation.owner, lazyAssociation, primaryKey.record)
        if (value == null) value = [null]
        if (!(value instanceof Collection)) {
            value = [value]
        }
        return new OrientResultList(0, value.iterator(), (OrientEntityPersister) session.getPersister(indexedEntity)).toList()
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
