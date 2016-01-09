package org.grails.datastore.gorm

import com.orientechnologies.orient.core.db.ODatabase
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import grails.core.DefaultGrailsApplication
import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.DomainEventListener
import org.grails.datastore.gorm.orientdb.*
import org.grails.datastore.gorm.orientdb.graph.*
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import org.springframework.context.support.GenericApplicationContext

class Setup {

    static connectionDetails = [username: "admin", password: "admin", url: "memory:test"]
    static ODatabase db
    static OrientDbDatastore datastore
    static OrientDbSession session

    static destroy() {
        datastore?.destroy()
        datastore = null
        session = null
        db = null
    }

    static Session setup(nativeClasses) {
        db = new ODatabaseDocumentTx("memory:test")
        if (!db.exists()) {
            db = db.create()
        } else {
            db.open("admin", "admin")
            db.drop()
            db = new ODatabaseDocumentTx("memory:test").create()
        }
        def classes = [Person, Pet, PetType, Parent, Child]
        def ctx = new GenericApplicationContext()
        ctx.refresh()
        MappingContext mappingContext = new OrientDbMappingContext({})
        datastore = new OrientDbDatastore(mappingContext, ctx)

        for (Class cls in classes) {
            mappingContext.addPersistentEntity(cls)
        }

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

}
