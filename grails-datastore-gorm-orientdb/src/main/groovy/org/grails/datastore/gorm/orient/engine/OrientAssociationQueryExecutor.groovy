package org.grails.datastore.gorm.orient.engine

import com.orientechnologies.orient.core.db.record.OIdentifiable
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.engine.AssociationQueryExecutor
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.query.Query

/**
 * Executor that handles relations from inverse side.
 */
@CompileStatic
class OrientAssociationQueryExecutor implements AssociationQueryExecutor<OIdentifiable, Object> {
    OIdentifiable key
    Association association
    Session session

    OrientAssociationQueryExecutor(OIdentifiable key, Association association, Session session) {
        this.key = key
        this.association = association
        this.session = session
    }



    @Override
    List<Object> query(OIdentifiable primaryKey) {
        // for a bidirectional one-to-many we use the foreign key to query the inverse side of the association
        Association inverseSide = association.getInverseSide();
        Query query = session.createQuery(association.getAssociatedEntity().getJavaClass());
        query.eq(inverseSide.getName(), primaryKey);
        return query.list();
    }

    @Override
    PersistentEntity getIndexedEntity() {
        return association.associatedEntity
    }

    @Override
    boolean doesReturnKeys() {
        return false
    }
}
