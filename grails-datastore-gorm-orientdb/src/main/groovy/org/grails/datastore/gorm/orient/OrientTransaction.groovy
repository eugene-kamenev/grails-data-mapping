package org.grails.datastore.gorm.orient

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.transactions.Transaction

/**
 * OrientDB Transaction implementation
 *
 * @author eugenekamenev
 */
@CompileStatic
class OrientTransaction implements Transaction<ODatabaseDocumentTx> {

    static final String DEFAULT_NAME = "OrientDbTransaction"

    boolean active = true

    /**
     * Current documentTx instance
     */
    ODatabaseDocumentTx documentTx

    boolean rollbackOnly = false

    OrientTransaction(ODatabaseDocumentTx documentTx) {
        this.documentTx = documentTx.begin()
    }

    /**
     * Transaction commit
     */
    @Override
    void commit() {
        if(isActive() && !rollbackOnly) {
            documentTx.transaction.commit()
        }
    }

    /**
     * Transaction rollback
     */
    @Override
    void rollback() {
        if(isActive()) {
            documentTx.transaction.rollback(true, -1)
            rollbackOnly = true
            active = false
        }
    }

    /**
     * Transaction rollback
     */
    void rollbackOnly() {
        if(isActive()) {
            rollbackOnly = true
            documentTx.transaction.rollback(true, -1)
            active = false
        }
    }

    /**
     * Native documentTx instance
     * @return
     */
    @Override
    ODatabaseDocumentTx getNativeTransaction() {
        return documentTx
    }

    /**
     * If transaction is active
     * @return
     */
    @Override
    boolean isActive() {
        return active
    }

    /**
     * Transaction timeout not supported
     *
     * @param timeout The timeout
     */
    @Override
    void setTimeout(int timeout) {
        throw new UnsupportedOperationException()
    }
}