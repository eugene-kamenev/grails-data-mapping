package org.grails.datastore.gorm.orientdb.engine

import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.query.OResultSet
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.query.AbstractResultList

import javax.persistence.LockModeType

@CompileStatic
class OrientDbResultList extends AbstractResultList {
    final protected transient OrientDbEntityPersister entityPersister;

    protected final LockModeType lockMode

    OrientDbResultList(int offset, OResultSet cursor, OrientDbEntityPersister entityPersister) {
        super(offset, (Iterator) cursor.iterator())
        this.entityPersister = entityPersister
        this.lockMode = javax.persistence.LockModeType.NONE;
    }

    OrientDbResultList(int offset, Integer size, Iterator cursor, OrientDbEntityPersister entityPersister) {
        super(offset, size, cursor)
        this.entityPersister = entityPersister
        this.lockMode = javax.persistence.LockModeType.NONE;
    }

    @Override
    protected Object nextDecoded() {
        def next = cursor.next()

        if (next instanceof ODocument) {
            def doc = (ODocument) next
            if (doc.className != null) {
                println "Returning $next"
                return doc
            } else {
                if (doc.fields() == 1) {
                    return doc.fieldValues()[0]
                } else {
                    return doc.fieldValues().toList()
                }
            }
        }
        return next
    }

    @Override
    protected Object convertObject(Object o) {
        println "converting Object $o"
        if (o instanceof ODocument) {
            final ODocument dbObject = (ODocument) o;
            def instance = entityPersister.unmarshallEntity(entityPersister.persistentEntity, dbObject);
            println "returning instance $instance"
            return instance;
        } else {
            println "not a document was in ResultList results"
        }
        return o;
    }

    @Override
    void close() throws IOException {
    }
}
