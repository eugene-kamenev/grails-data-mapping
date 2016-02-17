package org.grails.datastore.gorm.orient.engine

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.core.impl.PendingOperationAdapter
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.model.types.Association

@CompileStatic
class OrientPendingRelationshipDelete extends PendingOperationAdapter<Object, Serializable> {

    OrientPendingRelationshipDelete(EntityAccess parent, Association association, Collection<Serializable> pendingInserts) {
        super(parent.persistentEntity, null, null)
    }

    @Override
    void run() {

    }
}
