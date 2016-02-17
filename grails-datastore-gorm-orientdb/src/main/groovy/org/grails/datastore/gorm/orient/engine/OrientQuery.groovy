package org.grails.datastore.gorm.orient.engine

import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.query.OResultSet
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery
import groovy.transform.CompileStatic
import groovy.util.logging.Log
import org.grails.datastore.gorm.orient.OrientPersistentEntity
import org.grails.datastore.gorm.orient.OrientSession
import org.grails.datastore.gorm.orient.collection.OrientResultList
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.mapping.query.api.QueryArgumentsAware
import org.springframework.dao.InvalidDataAccessApiUsageException
/**
 * OrientDB GORM query implementation
 *
 * @author eugenekamenev
 */
@CompileStatic
@Log
class OrientQuery extends org.grails.datastore.mapping.query.Query implements QueryArgumentsAware {

    protected Map queryArgs = [:]

    OrientQuery(Session session, PersistentEntity entity) {
        super(session, entity)
        if (session == null) {
            throw new InvalidDataAccessApiUsageException("Argument session cannot be null");
        }
        if (entity == null) {
            throw new InvalidDataAccessApiUsageException("No persistent entity specified");
        }
    }

    @Override
    protected List executeQuery(PersistentEntity entity, Query.Junction criteria) {
        def orientEntity = (OrientPersistentEntity) entity
        List list = []
        if (orientEntity.graph) {
            list = executeQueryForGraph(orientEntity, criteria)
        } else {
            list = executeQueryForDocument(orientEntity, criteria)
        }
        def persister = (OrientEntityPersister) getSession().getPersister(orientEntity)
        return new OrientResultList(0, (OResultSet) list, persister)
    }

    private List executeQueryForDocument(OrientPersistentEntity entity, Query.Junction criteria) {
        OrientQueryBuilder builder = new OrientQueryBuilder(entity)
        if (max > 0) {
            queryArgs.max = max
        }
        if (offset > 0) {
            queryArgs.offset = offset
        }
        if (!orderBy.isEmpty()) {
            queryArgs.sort = [:]
            for(order in orderBy) {
                queryArgs.sort[order.property] = order.direction
            }
        }
        builder.build(projections, criteria, queryArgs)
        log.info("EXECUTING OrientDB query: ${builder.toString()} \n with params $builder.namedParameters")
        return session.documentTx.query(new OSQLSynchQuery(builder.toString()), builder.namedParameters)
    }

    private List executeQueryForGraph(OrientPersistentEntity entity, Query.Junction criteria) {
        // for now executing Document Version as it works for both
        return executeQueryForDocument(entity, criteria)
    }

    @Override
    Object singleResult() {
        def firstResult = super.singleResult()
        if (firstResult instanceof ODocument) {
            return firstResult.fieldValues().toList()
        }
        return firstResult
    }

    OrientSession getSession() {
        (OrientSession) super.getSession()
    }

    @Override
    OrientPersistentEntity getEntity() {
        (OrientPersistentEntity) super.getEntity()
    }

    @Override
    void setArguments(@SuppressWarnings("rawtypes") Map arguments) {
        this.queryArgs = arguments
    }
}
