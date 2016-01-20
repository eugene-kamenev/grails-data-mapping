package org.grails.datastore.gorm.orient.collection

import com.orientechnologies.orient.core.db.record.OIdentifiable
import com.orientechnologies.orient.core.record.impl.ODocument
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.orient.OrientPersistentEntity
import org.grails.datastore.gorm.orient.OrientDbSession
import org.grails.datastore.gorm.orient.engine.OrientEntityPersister
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.model.types.Association

@CompileStatic
class OrientLinkedSet implements Set {
    private Collection delegate

    private OIdentifiable nativeEntry
    private Association association
    private EntityAccess entityAccess
    private OrientDbSession session
    private OrientPersistentEntity holder

    OrientLinkedSet(EntityAccess entityAccess, OrientDbSession session, Association association, OIdentifiable nativeEntry) {
        this.association = association
        this.nativeEntry = nativeEntry
        this.entityAccess = entityAccess
        this.session = session
        this.holder = (OrientPersistentEntity) entityAccess.persistentEntity
    }

    private void initializeLazy() {
        if (delegate == null) {
            def tempList = []
            def record = nativeEntry.record
            if (record instanceof ODocument) {
                def persister = (OrientEntityPersister) session.getPersister(association.associatedEntity)
                tempList = ((Iterable<OIdentifiable>) record.field(holder.getNativePropertyName(association.name))).collect {OIdentifiable id ->
                    persister.unmarshallEntity((OrientPersistentEntity) association.associatedEntity, id)
                }
            }
            delegate = tempList
        }
    }

    @Override
    int size() {
        initializeLazy()
        return delegate.size()
    }

    @Override
    boolean isEmpty() {
        initializeLazy()
        return delegate.isEmpty()
    }

    @Override
    boolean contains(Object o) {
        initializeLazy()
        return delegate.contains(o)
    }

    @Override
    Iterator iterator() {
        initializeLazy()
        return delegate.iterator()
    }

    @Override
    Object[] toArray() {
        initializeLazy()
        return delegate.toArray()
    }

    @Override
    Object[] toArray(Object[] a) {
        initializeLazy()
        return delegate.toArray(a)
    }

    @Override
    boolean add(Object o) {
        return delegate.add(o)
    }

    @Override
    boolean remove(Object o) {
        initializeLazy()
        return delegate.remove(o)
    }

    @Override
    boolean containsAll(Collection c) {
        initializeLazy()
        return delegate.contains(c)
    }

    @Override
    boolean addAll(Collection c) {
        initializeLazy()
        return delegate.addAll(c)
    }

    @Override
    boolean removeAll(Collection c) {
        initializeLazy()
        return delegate.removeAll(c)
    }

    @Override
    boolean retainAll(Collection c) {
        initializeLazy()
        return delegate.retainAll(c)
    }

    @Override
    void clear() {
        initializeLazy()
        delegate.clear()
    }
}
