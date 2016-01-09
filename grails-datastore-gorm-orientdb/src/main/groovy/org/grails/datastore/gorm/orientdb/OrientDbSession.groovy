package org.grails.datastore.gorm.orientdb

import com.orientechnologies.orient.core.db.OPartitionedDatabasePool
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.tinkerpop.blueprints.impls.orient.OrientGraph
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.orientdb.engine.OrientDbEntityPersister
import org.grails.datastore.mapping.core.AbstractSession
import org.grails.datastore.mapping.engine.Persister
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.transactions.SessionOnlyTransaction
import org.grails.datastore.mapping.transactions.Transaction
import org.springframework.context.ApplicationEventPublisher
/**
 * Represents OrientDB GORM Session implementation
 */
@CompileStatic
class OrientDbSession extends AbstractSession<OPartitionedDatabasePool> {

    protected final OPartitionedDatabasePool connectionPool
    protected ODatabaseDocumentTx currentDocumentConnection

    OrientDbSession(OrientDbDatastore datastore, MappingContext mappingContext, ApplicationEventPublisher publisher, boolean stateless, OPartitionedDatabasePool connectionPool) {
        super(datastore, mappingContext, publisher, stateless)
        this.connectionPool = connectionPool
        this.documentTx
    }

    @Override
    protected Persister createPersister(Class cls, MappingContext mappingContext) {
        final PersistentEntity entity = mappingContext.getPersistentEntity(cls.getName());
        return (entity != null ) ? new OrientDbEntityPersister(mappingContext, entity, this, publisher) : null;
    }

    @Override
    protected Transaction beginTransactionInternal() {
        return new SessionOnlyTransaction<OPartitionedDatabasePool>(getNativeInterface(), this);
    }

    OrientGraph getGraph() {
        def currentActiveGraph = OrientGraph.activeGraph
        if (currentActiveGraph instanceof OrientGraph) return (OrientGraph) currentActiveGraph;
        new OrientGraph(documentTx)
    }

    ODatabaseDocumentTx getDocumentTx() {
        if (currentDocumentConnection == null) {
            currentDocumentConnection = nativeInterface.acquire()
        }
        currentDocumentConnection
    }

    @Override
    OPartitionedDatabasePool getNativeInterface() {
        return this.connectionPool
    }

    @Override
    void clear() {
        super.clear()
    }

    @Override
    void flush() {
       super.flush()
    }

   @Override
    OrientDbDatastore getDatastore() {
        return (OrientDbDatastore) super.getDatastore()
    }
}
