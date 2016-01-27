package org.grails.datastore.gorm.orient

import groovy.transform.CompileStatic
import javassist.util.proxy.MethodHandler
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.engine.AssociationQueryExecutor
import org.grails.datastore.mapping.proxy.JavassistProxyFactory

/**
 * OrientProxyFactory implementation, needed to alter some logic for edges associations
 *
 * @author eugenekamenev
 */
@CompileStatic
class OrientProxyFactory extends JavassistProxyFactory {
    @Override
    protected <K extends Serializable, T> MethodHandler createMethodHandler(Session session, AssociationQueryExecutor<K, T> executor, K associationKey) {
        return new OrientAssociationQueryProxyHandler(session, executor, associationKey)
    }
}
