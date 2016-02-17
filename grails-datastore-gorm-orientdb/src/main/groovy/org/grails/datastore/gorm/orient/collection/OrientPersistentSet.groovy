package org.grails.datastore.gorm.orient.collection

import com.orientechnologies.orient.core.db.record.OIdentifiable
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.orient.OrientSession
import org.grails.datastore.gorm.orient.engine.OrientAssociationQueryExecutor
import org.grails.datastore.mapping.collection.PersistentSet
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.ToMany

@CompileStatic
class OrientPersistentSet extends PersistentSet {
    protected final EntityAccess parentAccess
    protected final Association association
    protected final @Delegate OrientAdapter adapter

    OrientPersistentSet(Collection keys, OrientSession session, EntityAccess parentAccess, ToMany association) {
        super(keys, association.associatedEntity.javaClass, session)
        this.parentAccess = parentAccess
        this.association = association
        adapter = new OrientAdapter(session, association, parentAccess)
        setProxyEntities(association.isLazy())
    }

    OrientPersistentSet(Serializable associationKey, OrientSession session, EntityAccess parentAccess, ToMany association) {
        super(associationKey, session, new OrientAssociationQueryExecutor((OIdentifiable) associationKey, association, session))
        this.parentAccess = parentAccess
        this.association = association
        this.adapter = new OrientAdapter(session, association, parentAccess)
       // setProxyEntities(association.isLazy())
    }

    @Override
    boolean addAll(Collection c) {
        def added = super.addAll(c)
        if(added) {
            for( o in c ) {
                adaptGraphUponAdd(o, currentlyInitializing())
            }
        }

        return added
    }

    @Override
    boolean add(Object o) {
        def added = super.add(o)
        if(added) {
            adaptGraphUponAdd(o, currentlyInitializing())
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
            adaptGraphUponRemove(o, currentlyInitializing())
        }
        return removed
    }

    @Override
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

    @Override
    void markDirty() {
        if(!currentlyInitializing()) {
            ((DirtyCheckable)parentAccess.entity).markDirty(association.getName())
            super.markDirty()
        }
    }
}
