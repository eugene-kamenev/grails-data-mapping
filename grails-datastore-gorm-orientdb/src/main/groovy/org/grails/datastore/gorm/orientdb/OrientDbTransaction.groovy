package org.grails.datastore.gorm.orientdb

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.transactions.Transaction
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.DefaultTransactionDefinition

@CompileStatic
class OrientDbTransaction implements Transaction<ODatabaseDocumentTx>, Closeable {

    static final String DEFAULT_NAME = "OrientDbTransaction"

    boolean active = true

    ODatabaseDocumentTx documentTx
    boolean rollbackOnly = false
    final boolean sessionCreated
    TransactionDefinition transactionDefinition

    OrientDbTransaction(ODatabaseDocumentTx documentTx, TransactionDefinition transactionDefinition = new DefaultTransactionDefinition(), boolean sessionCreated = false) {
        this.transactionDefinition = transactionDefinition
        this.documentTx = documentTx.begin()
        this.sessionCreated = sessionCreated
        println "new transaction started"
    }

    @Override
    void commit() {
        if(isActive() && !rollbackOnly) {
            documentTx.transaction.commit()
        }
    }

    @Override
    void rollback() {
        if(isActive()) {
            documentTx.transaction.rollback(true, -1)
            rollbackOnly = true
            active = false
        }
    }

    void rollbackOnly() {
        if(isActive()) {
            rollbackOnly = true
            documentTx.transaction.rollback(true, -1)
            active = false
        }
    }

    @Override
    ODatabaseDocumentTx getNativeTransaction() {
        return documentTx
    }

    @Override
    boolean isActive() {
        return active
    }

    @Override
    void setTimeout(int timeout) {
        throw new UnsupportedOperationException()
    }

    @Override
    void close() throws IOException {
        if(!rollbackOnly && isActive()) {
            println "commiting on close"
            documentTx.transaction.commit()
            documentTx.close()
            active = false
        }
    }
}