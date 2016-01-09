package org.grails.datastore.gorm.orientdb.engine

import com.orientechnologies.orient.core.record.impl.ODocument
import com.tinkerpop.blueprints.impls.orient.OrientElement
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.query.AbstractResultList
import org.grails.datastore.mapping.model.types.Association

import javax.persistence.LockModeType

@CompileStatic
class OrientDbResultList extends AbstractResultList {
    private static final Map<Association, Object> EMPTY_ASSOCIATIONS = Collections.<Association, Object> emptyMap()
    private static final Map<String, Object> EMPTY_RESULT_DATA = Collections.<String, Object> emptyMap()

    final protected transient  OrientDbEntityPersister entityPersister;

    protected transient Map<Association, Object> initializedAssociations = EMPTY_ASSOCIATIONS

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



    /**
     * Set any already initialized associations to avoid extra proxy queries
     *
     * @param initializedAssociations
     */
    void setInitializedAssociations(Map<Association, Object> initializedAssociations) {
        this.initializedAssociations = initializedAssociations
    }

    @Override
    protected Object nextDecoded() {
        return nextDecodedInternal()
    }

    private Object nextDecodedInternal() {
        def next = cursor.next()
        if (next instanceof ODocument) {
            return decodeFromDocument(next)
        }
        if (next instanceof OrientElement) {
            return decodeFromGraph(next)
        }
        next
    }

    private Object decodeFromGraph(OrientElement element) {
        return entityPersister.unmarshallFromGraph(entityPersister.persistentEntity, element)
    }

    private Object decodeFromDocument(ODocument next) {
        if (next.className != null) {
            return entityPersister.unmarshallDocument(entityPersister.persistentEntity, next)
        } else {
            if (next.fields() == 1) {
                return next.fieldValues()[0]
            }
        }
        return next
    }

    @Override
    void close() throws IOException {
    }
}
