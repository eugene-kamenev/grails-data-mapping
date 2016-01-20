package org.grails.datastore.gorm.orient

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.tinkerpop.blueprints.impls.orient.OrientGraph
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.orient.engine.OrientEntityPersister
import org.grails.datastore.mapping.core.AbstractSession
import org.grails.datastore.mapping.engine.Persister
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.transactions.Transaction
import org.springframework.context.ApplicationEventPublisher

/**
 * Represents OrientDB GORM Session implementation
 */
@CompileStatic
class OrientDbSession extends AbstractSession<ODatabaseDocumentTx> {

    protected ODatabaseDocumentTx currentDocumentConnection
    protected OrientGraph currentActiveGraph

    OrientDbSession(OrientDatastore datastore, MappingContext mappingContext, ApplicationEventPublisher publisher, boolean stateless, ODatabaseDocumentTx connection) {
        super(datastore, mappingContext, publisher, stateless)
        currentDocumentConnection = connection
    }

    @Override
    protected Persister createPersister(Class cls, MappingContext mappingContext) {
        final PersistentEntity entity = mappingContext.getPersistentEntity(cls.getName());
        return (entity != null) ? new OrientEntityPersister(mappingContext, entity, this, publisher) : null;
    }

    @Override
    protected Transaction beginTransactionInternal() {
        new OrientTransaction(documentTx)
    }

    OrientGraph getGraph() {
        if (currentActiveGraph == null) {
            currentActiveGraph = new OrientGraph(documentTx)
        }
        currentActiveGraph
    }

    @Override
    void flush() {
        if (transaction.active && !transaction.rollbackOnly) {
            transaction.commit()
        }
        super.flush()
    }

    @Override
    void clear() {
        super.clear()
        if (!transaction.active && transaction.rollbackOnly) {
            transaction.rollbackOnly()
        }

    }

    @Override
    void disconnect() {
        if (isConnected()) {
            super.disconnect()
            if (currentActiveGraph != null && !currentActiveGraph.closed) {
                currentActiveGraph.shutdown(false, false)
            }
            if (!documentTx.closed) {
                documentTx.close()
            }
        }
    }

    ODatabaseDocumentTx getDocumentTx() {
        currentDocumentConnection
    }

    @Override
    ODatabaseDocumentTx getNativeInterface() {
        return this.currentDocumentConnection
    }

    @Override
    OrientTransaction getTransaction() {
        (OrientTransaction) super.getTransaction()
    }

    @Override
    OrientDatastore getDatastore() {
        return (OrientDatastore) super.getDatastore()
    }
}
