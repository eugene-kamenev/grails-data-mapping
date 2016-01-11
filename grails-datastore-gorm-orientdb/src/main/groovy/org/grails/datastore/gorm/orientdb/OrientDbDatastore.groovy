package org.grails.datastore.gorm.orientdb

import com.orientechnologies.orient.core.db.OPartitionedDatabasePool
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.core.AbstractDatastore
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.core.StatelessDatastore
import org.grails.datastore.mapping.document.DocumentDatastore
import org.grails.datastore.mapping.graph.GraphDatastore
import org.grails.datastore.mapping.model.MappingContext
import org.springframework.beans.factory.DisposableBean
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.PropertyResolver
/**
 * OrientDB GORM datastore implementation
 *
 * Can be configured to be used in with different connection modes, like:
 * memory/plocal/binary, by default in-memory embedded db will be used
 *
 * TODO: add dynamic username/password provider, as orientdb supports row-level security
 * @author @eugenekamenev
 */
@CompileStatic
class OrientDbDatastore extends AbstractDatastore implements DisposableBean, StatelessDatastore, GraphDatastore, DocumentDatastore {

    static final String KEY_ORIENTDB_URL = 'grails.orientdb.url'
    static final String KEY_ORIENTDB_USERNAME = 'grails.orientdb.user'
    static final String KEY_ORIENTDB_PASSWORD = 'grails.orientdb.password'
    static final String KEY_ORIENTDB_POOL_SIZE = 'grails.orientdb.pool'

    static final String DEFAULT_ORIENTDB_URL = 'memory:test'
    static final String DEFAULT_ORIENTDB_USERNAME = 'admin'
    static final String DEFAULT_ORIENTDB_PASSWORD = 'admin'
    static final String DEFAULT_ORIENTDB_MODE = 'memory'
    static final String DEFAULT_ORIENTDB_POOL_SIZE = 10

    protected OPartitionedDatabasePool orientPool

    OrientDbDatastore(MappingContext mappingContext) {
        super(mappingContext)
        createDatabasePool()
    }

    OrientDbDatastore(MappingContext mappingContext, Map<String, Object> connectionDetails, ConfigurableApplicationContext ctx) {
        super(mappingContext, connectionDetails, ctx)
        createDatabasePool(connectionDetails)
    }

    OrientDbDatastore(MappingContext mappingContext, PropertyResolver connectionDetails, ConfigurableApplicationContext ctx) {
        super(mappingContext, connectionDetails, ctx)
        createDatabasePool(getConnectionProperties(connectionDetails))
    }

    OrientDbDatastore(MappingContext mappingContext, ConfigurableApplicationContext ctx, OPartitionedDatabasePool orientPool) {
        super(mappingContext, ctx.getEnvironment(), ctx)
        this.orientPool = orientPool
    }

    private Map getConnectionProperties(PropertyResolver propertyResolver) {
        [url: connectionDetails?.getProperty(KEY_ORIENTDB_URL) ?: DEFAULT_ORIENTDB_URL,
         userName: connectionDetails?.getProperty(KEY_ORIENTDB_USERNAME) ?: DEFAULT_ORIENTDB_USERNAME,
         password: connectionDetails?.getProperty(KEY_ORIENTDB_PASSWORD) ?: DEFAULT_ORIENTDB_PASSWORD,
         size: connectionDetails?.getProperty(KEY_ORIENTDB_POOL_SIZE) ?: DEFAULT_ORIENTDB_POOL_SIZE]
    }

    private createDatabasePool(Map connectionDetails = null) {
        if (!this.orientPool) {
            def connectionProperties = connectionDetails
            if (!connectionDetails) connectionProperties = getConnectionProperties(null)
            def poolClass = Class.forName("com.orientechnologies.orient.core.db.OPartitionedDatabasePool", true, Thread.currentThread().getContextClassLoader());
            this.orientPool = (OPartitionedDatabasePool) poolClass.newInstance(connectionProperties.url, connectionProperties.userName, connectionProperties.password, connectionProperties.size);
        }
    }

    @Override
    protected Session createSession(PropertyResolver connectionDetails) {
        new OrientDbSession(this, mappingContext, getApplicationEventPublisher(), false, this.orientPool.acquire())
    }

    @Override
    void destroy() throws Exception {
        super.destroy()
        if (this.orientPool) {
            this.orientPool.close()
        }
    }
}
