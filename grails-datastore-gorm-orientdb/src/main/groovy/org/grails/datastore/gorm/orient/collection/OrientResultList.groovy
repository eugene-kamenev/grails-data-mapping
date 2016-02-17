package org.grails.datastore.gorm.orient.collection

import com.orientechnologies.orient.core.db.record.OIdentifiable
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.query.OResultSet
import com.tinkerpop.blueprints.impls.orient.OrientVertex
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.orient.engine.OrientEntityPersister
import org.grails.datastore.gorm.query.AbstractResultList

import javax.persistence.LockModeType

/**
 * OrientDB result list implementation
 *
 * @author eugenekamenev
 */
@CompileStatic
class OrientResultList extends AbstractResultList {
    final protected transient OrientEntityPersister entityPersister;

    protected final LockModeType lockMode

    OrientResultList(int offset, Iterator cursor, OrientEntityPersister entityPersister) {
        super(offset, cursor)
        this.entityPersister = entityPersister
        this.lockMode = javax.persistence.LockModeType.NONE;
    }

    OrientResultList(int offset, OResultSet cursor, OrientEntityPersister entityPersister) {
        super(offset, (Iterator) cursor.iterator())
        this.entityPersister = entityPersister
        this.lockMode = javax.persistence.LockModeType.NONE;
    }

    OrientResultList(int offset, Integer size, Iterator cursor, OrientEntityPersister entityPersister) {
        super(offset, size, cursor)
        this.entityPersister = entityPersister
        this.lockMode = javax.persistence.LockModeType.NONE;
    }

    @Override
    protected Object nextDecoded() {
        def next = cursor.next()
        if (next instanceof OrientVertex) {
            next = ((OrientVertex)next).record
        }
        if (next instanceof ODocument) {
            def doc = (ODocument) next
            if (doc.className != null) {
                return doc
            } else {
                if (doc.fields() == 1) {
                    return doc.fieldValues()[0]
                } else {
                    return doc.fieldValues().toList()
                }
            }
        }
        if (next instanceof OIdentifiable) {
            return (((OIdentifiable) next).identity).record.load() as ODocument
        }
        return next
    }

    @Override
    protected Object convertObject(Object o) {
        if (o instanceof ODocument) {
            final ODocument dbObject = (ODocument) o;
            dbObject.setLazyLoad(false)
            def instance = entityPersister.unmarshallEntity(entityPersister.persistentEntity, dbObject);
            return instance;
        }
        return o;
    }

    @Override
    void close() throws IOException {
    }
}
