package org.grails.datastore.gorm.orientdb

import com.orientechnologies.orient.core.db.OPartitionedDatabasePoolFactory
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
    static final String KEY_ORIENTDB_CONNECTION_MODE = 'grails.orientdb.connection_mode'

    static final String DEFAULT_ORIENTDB_URL = 'memory:test'
    static final String DEFAULT_ORIENTDB_USERNAME = 'admin'
    static final String DEFAULT_ORIENTDB_PASSWORD = 'admin'
    static final String DEFAULT_ORIENTDB_MODE = 'memory'

    protected OPartitionedDatabasePoolFactory orientFactory

    /**
     * Configures a new {@link OrientDbDatastore} for the given arguments
     *
     * @param mappingContext The {@link MappingContext} which contains information about the mapped classes
     * @param configuration The configuration for the datastore
     * @param applicationContext The Spring ApplicationContext
     */
    public OrientDbDatastore(MappingContext mappingContext, PropertyResolver configuration, ConfigurableApplicationContext applicationContext) {
        super(mappingContext, configuration, applicationContext);
        this.orientFactory = createDatabasePool(configuration);
    }

    /**
     * Configures {@link OrientDbDatastore} for the given arguments
     *
     * @param mappingContext The {@link MappingContext} which contains information about the mapped classes
     * @param configuration The configuration for the datastore
     * @param applicationContext The Spring ApplicationContext
     */
    public OrientDbDatastore(MappingContext mappingContext, PropertyResolver configuration, ConfigurableApplicationContext applicationContext, OPartitionedDatabasePoolFactory graphDatabaseService) {
        super(mappingContext, configuration, applicationContext);
        this.orientFactory = graphDatabaseService;
    }

    /**
     * @see {@link #OrientDbDatastore(MappingContext, PropertyResolver, ConfigurableApplicationContext)}
     */
    public OrientDbDatastore(MappingContext mappingContext, ConfigurableApplicationContext applicationContext) {
        this(mappingContext, applicationContext.getEnvironment(), applicationContext);
    }

    /**
     * @see {@link #OrientDbDatastore(MappingContext, PropertyResolver, ConfigurableApplicationContext)}
     */
    public OrientDbDatastore(MappingContext mappingContext, ConfigurableApplicationContext applicationContext, OPartitionedDatabasePoolFactory orientDbPoolFactory) {
        this(mappingContext, applicationContext.getEnvironment(), applicationContext, orientDbPoolFactory);
    }

    private OPartitionedDatabasePoolFactory createDatabasePool(PropertyResolver configuration) {
        this.orientFactory = (OPartitionedDatabasePoolFactory) Class.forName("com.orientechnologies.orient.core.db.OPartitionedDatabasePoolFactory", true, Thread.currentThread().getContextClassLoader()).newInstance();
    }

    @Override
    protected Session createSession(PropertyResolver connectionDetails) {
        def url = connectionDetails.getProperty(KEY_ORIENTDB_URL) ?: DEFAULT_ORIENTDB_URL
        def userName = connectionDetails.getProperty(KEY_ORIENTDB_USERNAME) ?: DEFAULT_ORIENTDB_USERNAME
        def password = connectionDetails.getProperty(KEY_ORIENTDB_PASSWORD) ?: DEFAULT_ORIENTDB_PASSWORD
        def connection = this.orientFactory.get(url, userName, password)
        new OrientDbSession(this, mappingContext, getApplicationEventPublisher(), false, connection)
    }

    @Override
    void destroy() throws Exception {
        super.destroy()
        if (this.orientFactory) {
            this.orientFactory.close()
        }
    }
}
