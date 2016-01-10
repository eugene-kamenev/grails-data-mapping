package org.grails.datastore.gorm

import com.orientechnologies.orient.core.db.ODatabase
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.sql.OCommandSQL
import grails.core.DefaultGrailsApplication
import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.DomainEventListener
import org.grails.datastore.gorm.orientdb.OrientDbDatastore
import org.grails.datastore.gorm.orientdb.OrientDbMappingContext
import org.grails.datastore.gorm.orientdb.OrientDbPersistentEntity
import org.grails.datastore.gorm.orientdb.OrientDbSession
import org.grails.datastore.gorm.orientdb.document.*
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
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
        def classes = [Person, Pet, PetType, Parent, Child, TestEntity, Face, Nose, Highway, Book]
        def ctx = new GenericApplicationContext()
        ctx.refresh()
        MappingContext mappingContext = new OrientDbMappingContext({})
        datastore = new OrientDbDatastore(mappingContext, ctx)

        for (Class cls in classes) {
            mappingContext.addPersistentEntity(cls)
        }
        mappingContext.getPersistentEntities().each { PersistentEntity e ->
            def orientEntity = (OrientDbPersistentEntity) e
            if (orientEntity.isVertex()) {
                db.command(new OCommandSQL("CREATE CLASS $orientEntity.className extends V"))
            }
            if (orientEntity.isDocument()) {
                db.getMetadata().getSchema().createClass(orientEntity.className)
            }
            if (orientEntity.isEdge()) {
                db.command(new OCommandSQL("CREATE CLASS $orientEntity.className extends E"))
            }
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
