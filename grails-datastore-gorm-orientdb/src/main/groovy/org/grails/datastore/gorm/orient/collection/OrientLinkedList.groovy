package org.grails.datastore.gorm.orient.collection

import com.orientechnologies.orient.core.db.record.OIdentifiable
import com.orientechnologies.orient.core.record.impl.ODocument
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.orient.OrientPersistentEntity
import org.grails.datastore.gorm.orient.OrientSession
import org.grails.datastore.gorm.orient.engine.OrientEntityPersister
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.model.types.Association

/**
 * OrientDB Lazy linked list,
 * TODO: should be rewritten
 *
 * @author eugenekamenev
 */
@CompileStatic
class OrientLinkedList implements List {
    private List delegate

    private OIdentifiable nativeEntry
    private Association association
    private EntityAccess entityAccess
    private OrientSession session
    private OrientPersistentEntity holder

    OrientLinkedList(EntityAccess entityAccess, OrientSession session, Association association, OIdentifiable nativeEntry) {
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
                tempList = ((Iterable<OIdentifiable>) record.field(holder.getNativePropertyName(association.name))).collect { OIdentifiable id ->
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
    boolean addAll(int index, Collection c) {
        initializeLazy()
        return delegate.addAll(index.byteValue())
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

    @Override
    Object get(int index) {
        initializeLazy()
        return delegate.get(index)
    }

    @Override
    Object set(int index, Object element) {
        initializeLazy()
        return delegate.set(index, element)
    }

    @Override
    void add(int index, Object element) {
        initializeLazy()
        delegate.add(index, element)
    }

    @Override
    Object remove(int index) {
        initializeLazy()
        return delegate.remove((int) index)
    }

    @Override
    int indexOf(Object o) {
        initializeLazy()
        return delegate.indexOf(o)
    }

    @Override
    int lastIndexOf(Object o) {
        initializeLazy()
        return delegate.lastIndexOf(o)
    }

    @Override
    ListIterator listIterator() {
        initializeLazy()
        return delegate.listIterator()
    }

    @Override
    ListIterator listIterator(int index) {
        initializeLazy()
        return delegate.listIterator(index)
    }

    @Override
    List subList(int fromIndex, int toIndex) {
        initializeLazy()
        return delegate.subList(fromIndex, toIndex)
    }
}
