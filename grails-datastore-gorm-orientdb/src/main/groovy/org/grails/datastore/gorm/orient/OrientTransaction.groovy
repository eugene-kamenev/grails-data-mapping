package org.grails.datastore.gorm.orient

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.transactions.Transaction

@CompileStatic
class OrientTransaction implements Transaction<ODatabaseDocumentTx> {

    static final String DEFAULT_NAME = "OrientDbTransaction"

    boolean active = true

    ODatabaseDocumentTx documentTx
    boolean rollbackOnly = false

    OrientTransaction(ODatabaseDocumentTx documentTx) {
        this.documentTx = documentTx.begin()
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
}