package org.grails.datastore.gorm.orient

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.engine.AssociationQueryExecutor
import org.grails.datastore.mapping.proxy.AssociationQueryProxyHandler

/**
 * Needed to alter some logic for edges associations
 *
 * @author eugenekamenev
 */
@CompileStatic
class OrientAssociationQueryProxyHandler extends AssociationQueryProxyHandler {

    OrientAssociationQueryProxyHandler(Session session, AssociationQueryExecutor executor, Serializable associationKey) {
        super(session, executor, associationKey)
    }

    /**
     * Overriding because of edge usage, where we know vertex only id
     *
     * @param self
     * @return
     */
    @Override
    protected Object getProxyKey(Object self) {
        def key = super.getProxyKey(self)
        if (!key && target != null) {
            return getPropertyAfterResolving(target, this.executor.indexedEntity.getIdentity().name)
        }
        return key
    }
}
