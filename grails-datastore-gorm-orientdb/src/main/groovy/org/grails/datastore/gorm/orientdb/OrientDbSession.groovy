package org.grails.datastore.gorm.orientdb

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.tx.OTransaction
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
class OrientDbSession extends AbstractSession<ODatabaseDocumentTx> {

    protected ODatabaseDocumentTx currentDocumentConnection
    protected OrientGraph currentActiveGraph

    OrientDbSession(OrientDbDatastore datastore, MappingContext mappingContext, ApplicationEventPublisher publisher, boolean stateless, ODatabaseDocumentTx connection) {
        super(datastore, mappingContext, publisher, stateless)
        currentDocumentConnection = connection
    }

    @Override
    protected Persister createPersister(Class cls, MappingContext mappingContext) {
        final PersistentEntity entity = mappingContext.getPersistentEntity(cls.getName());
        return (entity != null ) ? new OrientDbEntityPersister(mappingContext, entity, this, publisher) : null;
    }

    @Override
    protected Transaction beginTransactionInternal() {
        return new SessionOnlyTransaction<OTransaction>(currentDocumentConnection.getTransaction(), this);
    }

    OrientGraph getGraph() {
        println "getting graph"
        if (currentActiveGraph == null) {
            currentActiveGraph = new OrientGraph(documentTx)
        }
        currentActiveGraph
    }

    ODatabaseDocumentTx getDocumentTx() {
        println "getting native interface"
        currentDocumentConnection
    }

    @Override
    ODatabaseDocumentTx getNativeInterface() {
        println "getting native interface"
        return this.currentDocumentConnection
    }

    @Override
    void clear() {
        println "session cleared"
        super.clear()
        if (currentActiveGraph != null) {
            if (!currentActiveGraph.closed) {
                println "graph shutdown call"
                currentActiveGraph.shutdown(false, false)
            }
        }
    }

    @Override
    void flush() {
       println "session flushed"
       super.flush()
        if (currentActiveGraph != null) {
            if (!currentActiveGraph.closed) {
                println "graph shutdown call"
                currentActiveGraph.shutdown(false, true)
            }
        }
       getTransaction().nativeTransaction.commit()
    }

    @Override
    SessionOnlyTransaction<OTransaction> getTransaction() {
        return (SessionOnlyTransaction<OTransaction>) super.getTransaction()
    }

    @Override
    OrientDbDatastore getDatastore() {
        return (OrientDbDatastore) super.getDatastore()
    }
}
