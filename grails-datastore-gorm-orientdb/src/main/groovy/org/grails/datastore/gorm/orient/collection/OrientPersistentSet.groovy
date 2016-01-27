package org.grails.datastore.gorm.orient.collection

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.collection.PersistentSet
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.engine.AssociationQueryExecutor

/**
 * OrientDB PersistentSet association query impl
 *
 * @author eugenekamenev
 */
@CompileStatic
class OrientPersistentSet extends PersistentSet {

    OrientPersistentSet(Serializable associationKey, Session session, AssociationQueryExecutor indexer) {
        super(associationKey, session, indexer)
    }
}
