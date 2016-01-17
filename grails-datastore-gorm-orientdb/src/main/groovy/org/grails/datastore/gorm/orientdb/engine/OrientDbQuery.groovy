package org.grails.datastore.gorm.orientdb.engine

import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.query.OResultSet
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.orientdb.OrientDbPersistentEntity
import org.grails.datastore.gorm.orientdb.OrientDbSession
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.mapping.query.api.QueryArgumentsAware
import org.springframework.dao.InvalidDataAccessApiUsageException

@CompileStatic
class OrientDbQuery extends org.grails.datastore.mapping.query.Query implements QueryArgumentsAware {

    protected Map queryArgs = [:]

    OrientDbQuery(Session session, PersistentEntity entity) {
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
        def orientEntity = (OrientDbPersistentEntity) entity
        List list = []
        if (orientEntity.graph) {
            list = executeQueryForGraph(orientEntity, criteria)
        } else {
            list = executeQueryForDocument(orientEntity, criteria)
        }
        def persister = (OrientDbEntityPersister) getSession().getPersister(orientEntity)
        println "query returned ${list.size()} result: \n $list"
        return new OrientDbResultList(0, (OResultSet) list, persister)
    }

    private List executeQueryForDocument(OrientDbPersistentEntity entity, Query.Junction criteria) {
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
        println "EXECUTING QUERY: " + builder.toString()
        return session.documentTx.query(new OSQLSynchQuery(builder.toString()))
    }

    private List executeQueryForGraph(OrientDbPersistentEntity entity, Query.Junction criteria) {
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

    OrientDbSession getSession() {
        (OrientDbSession) super.getSession()
    }

    @Override
    OrientDbPersistentEntity getEntity() {
        (OrientDbPersistentEntity) super.getEntity()
    }

    @Override
    void setArguments(@SuppressWarnings("rawtypes") Map arguments) {
        this.queryArgs = arguments
    }
}
