package org.grails.datastore.gorm.orientdb.engine

import com.orientechnologies.orient.core.record.impl.ODocument
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.query.AbstractResultList
import org.grails.datastore.mapping.core.SessionImplementor

import javax.persistence.LockModeType

@CompileStatic
class OrientDbResultList extends AbstractResultList {
    final protected transient OrientDbEntityPersister entityPersister;

    protected final LockModeType lockMode

    OrientDbResultList(int offset, Iterator cursor, OrientDbEntityPersister entityPersister) {
        super(offset, cursor)
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
        if (o instanceof ODocument) {
            final ODocument dbObject = (ODocument) o;
            Object id = dbObject.identity
            SessionImplementor session = (SessionImplementor) entityPersister.getSession();
            Class type = entityPersister.getPersistentEntity().getJavaClass();
            //Object instance = session.getCachedInstance(type, (Serializable) id);
            Object instance
            if (instance == null) {
                instance = entityPersister.createObjectFromNativeEntry(entityPersister.persistentEntity, (Serializable) id, dbObject);
                //session.cacheInstance(type, (Serializable) id, instance);
            }
            return instance;
        }
        return o;
    }

    @Override
    void close() throws IOException {
    }
}
