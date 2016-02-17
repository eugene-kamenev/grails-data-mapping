package org.grails.datastore.gorm

import com.orientechnologies.common.util.OCallable
import com.orientechnologies.orient.core.db.ODatabase
import com.orientechnologies.orient.core.db.OPartitionedDatabasePool
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph
import com.tinkerpop.blueprints.impls.orient.OrientGraph
import grails.core.DefaultGrailsApplication
import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.DomainEventListener
import org.grails.datastore.gorm.orient.*
import org.grails.datastore.gorm.orient.entity.graph.*
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import org.springframework.context.support.GenericApplicationContext

class Setup {

    static connectionDetails = [username: "admin", password: "admin", url: "memory:temp"]
    static OPartitionedDatabasePool poolFactory
    static ODatabase db
    static OrientDatastore datastore
    static OrientSession session

    static destroy() {
        datastore = null
        session = null
        poolFactory.acquire().drop()
        poolFactory.close()
        poolFactory = null
    }

    static Session setup(nativeClasses) {
        poolFactory = new OPartitionedDatabasePool(connectionDetails.url, connectionDetails.username, connectionDetails.password)
        poolFactory.autoCreate = true
        db = poolFactory.acquire()
        def classes = [Person, Pet, PetType, Parent, Child, TestEntity, Face, Nose, Highway, Book, ChildEntity, Country, City, Location, Publication, PlantCategory, Plant]
        // uncomment for mixed mapping suite
        /*classes = [org.grails.datastore.gorm.orient.entity.custom.Person,
                   org.grails.datastore.gorm.orient.entity.custom.City,
                   org.grails.datastore.gorm.orient.entity.custom.Country,
                   org.grails.datastore.gorm.orient.entity.custom.LivesIn,
                   org.grails.datastore.gorm.orient.entity.custom.HasFriend]*/
        def ctx = new GenericApplicationContext()
        ctx.refresh()
        def mappingContext = new OrientMappingContext({})
        for (Class cls in classes) {
            mappingContext.addPersistentEntity(cls)
        }
        datastore = new OrientDatastore(mappingContext, ctx, poolFactory)
        datastore.mappingContext.proxyFactory = new OrientProxyFactory()

        def grailsApplication = new DefaultGrailsApplication(classes as Class[], Setup.getClassLoader())
        grailsApplication.mainContext = ctx
        grailsApplication.initialise()

        def enhancer = new GormEnhancer(datastore, new DatastoreTransactionManager(datastore: datastore))
        enhancer.enhance()
        mappingContext.addMappingContextListener({e ->
            enhancer.enhance e
        } as MappingContext.Listener)
        ctx.addApplicationListener new DomainEventListener(datastore)
        ctx.addApplicationListener new AutoTimestampEventListener(datastore)
        // enable for debugging
        session = datastore.connect()
        session.beginTransaction()
        return session
    }

    static createVertexOrEdge(OrientGraph graph, OrientPersistentEntity orientEntity) {
        graph.executeOutsideTx(new OCallable<Object, OrientBaseGraph>() {
            public Object call(OrientBaseGraph iArgument) {
                if (orientEntity.edge) {
                    graph.createEdgeType(orientEntity.className).setClusterSelection('default');
                    return null
                }
                graph.createVertexType(orientEntity.className).setClusterSelection('default');
                null
            }
        });
    }

}
