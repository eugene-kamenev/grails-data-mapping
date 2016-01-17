package org.grails.datastore.gorm.orientdb.collection

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.collection.PersistentSet
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.engine.AssociationQueryExecutor

@CompileStatic
class OrientDbPersistentSet extends PersistentSet {

    OrientDbPersistentSet(Serializable associationKey, Session session, AssociationQueryExecutor indexer) {
        super(associationKey, session, indexer)
    }
}
