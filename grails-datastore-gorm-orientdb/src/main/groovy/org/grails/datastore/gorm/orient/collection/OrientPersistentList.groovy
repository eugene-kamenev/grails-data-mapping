package org.grails.datastore.gorm.orient.collection

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.collection.PersistentList
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.engine.AssociationQueryExecutor

@CompileStatic
class OrientPersistentList extends PersistentList {
    OrientPersistentList(Serializable associationKey, Session session, AssociationQueryExecutor indexer) {
        super(associationKey, session, indexer)
    }
}
