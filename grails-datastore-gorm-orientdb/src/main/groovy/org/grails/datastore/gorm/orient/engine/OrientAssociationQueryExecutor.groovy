package org.grails.datastore.gorm.orient.engine

import com.orientechnologies.orient.core.db.record.OIdentifiable
import com.orientechnologies.orient.core.db.record.OLazyRecordIterator
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.orient.collection.OrientResultList
import org.grails.datastore.gorm.orient.extensions.OrientGormHelper
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.engine.AssociationQueryExecutor
import org.grails.datastore.mapping.engine.internal.MappingUtils
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.query.Query
/**
 * Executor that handles relations from inverse side via query
 *
 * @author eugenekamenev
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
            primaryKey = this.key
        Association inverseSide = association.getInverseSide();
        if (inverseSide != null && OrientGormHelper.getOTypeForField(association) != OType.LINK) {
            Query query = session.createQuery(association.getAssociatedEntity().getJavaClass());
            query.eq(inverseSide.getName(), primaryKey);
            return query.list();
        } else {
            def record = (ODocument) primaryKey.record
            def result = record.field(MappingUtils.getTargetKey(association), OrientGormHelper.getOTypeForField(association))
            if (!result) {
                return []
            }
            if (Collection.isAssignableFrom(result.class)) {
                return new OrientResultList(0, new OLazyRecordIterator(((Collection<OIdentifiable>)result).iterator(), true), (OrientEntityPersister) session.getPersister(association.associatedEntity))
            } else {
                return new OrientResultList(0, [result].iterator(), (OrientEntityPersister) session.getPersister(association.associatedEntity))
            }
        }
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
