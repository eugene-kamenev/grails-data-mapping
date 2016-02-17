package org.grails.datastore.gorm.orient.collection

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.orient.OrientSession
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.datastore.mapping.dirty.checking.DirtyCheckingSet
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.model.types.Association

@CompileStatic
class OrientSet extends DirtyCheckingSet {
    final transient Association association
    final transient OrientSession session

    protected final @Delegate OrientAdapter graphAdapter

    OrientSet(EntityAccess parentAccess, Association association, Set delegate, OrientSession session) {
        super(delegate, (DirtyCheckable)parentAccess.entity, association.name)
        this.association = association
        this.session = session
        this.graphAdapter = graphAdapter
    }

    @Override
    boolean add(Object o) {

        def added = super.add(o)
        if(added) {
            adaptGraphUponAdd(o)
        }
        return added
    }

    @Override
    boolean addAll(Collection c) {
        def added = super.addAll(c)
        if(added) {
            for( o in c ) {
                adaptGraphUponAdd(o)
            }
        }

        return added
    }

    @Override
    boolean removeAll(Collection c) {
        def removed = super.removeAll(c)
        if(removed) {
            for(o in c) {
                adaptGraphUponRemove(o)
            }
        }
        return removed
    }

    @Override
    boolean remove(Object o) {
        def removed = super.remove(o)
        if(removed) {
            adaptGraphUponRemove(o)
        }
        return removed
    }

    boolean retainAll(Collection c) {
        return super.retainAll(c)
    }

    @Override
    Object[] toArray(Object[] a) {
        return super.toArray(a)
    }

    @Override
    boolean containsAll(Collection c) {
        return super.containsAll(c)
    }
}
