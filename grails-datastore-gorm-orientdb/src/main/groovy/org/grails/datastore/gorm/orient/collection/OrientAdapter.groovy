package org.grails.datastore.gorm.orient.collection

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.orient.OrientSession
import org.grails.datastore.gorm.orient.mapping.RelationshipUtils
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.ManyToMany
import org.grails.datastore.mapping.model.types.ToOne
import org.grails.datastore.mapping.reflect.EntityReflector

@CompileStatic
class OrientAdapter {
    final EntityAccess parentAccess
    final OrientSession session
    final Association association
    final boolean reversed
    final String relType
    final Class childType

    OrientAdapter(OrientSession session, Association association, EntityAccess parentAccess) {
        this.session = session
        this.association = association
        this.parentAccess = parentAccess
        reversed = RelationshipUtils.useReversedMappingFor(association)
        relType = RelationshipUtils.relationshipTypeUsedFor(association)
        childType = association.associatedEntity.javaClass
    }

    void adaptGraphUponRemove(Object o, boolean currentlyInitializing = false) {
        if(currentlyInitializing) return

        if (session.getMappingContext().getProxyFactory().isProxy(o)) {
            return;
        }
        if (!reversed) {
            def childAccess = session.mappingContext.getEntityReflector(association.getAssociatedEntity())
            session.addPendingRelationshipDelete((Serializable)parentAccess.getIdentifier(), association, (Serializable)childAccess.getIdentifier(o) )
        }
    }

    void adaptGraphUponAdd(Object t, boolean currentlyInitializing = false) {
        def proxyFactory = session.getMappingContext().getProxyFactory()
        if(currentlyInitializing) {
            // if the association is initializing then replace parent entities with non proxied version to prevent N+1 problem
            if (association.isBidirectional() && !proxyFactory.isProxy(t)) {
                def inverseSide = association.inverseSide
                if(inverseSide instanceof ToOne) {
                    EntityReflector target = session.mappingContext.getEntityReflector(association.getAssociatedEntity())
                    target.setProperty( t, inverseSide.name, parentAccess.entity )
                }
            }
        }
        else {

            if (proxyFactory.isProxy(t)) {
                if ( !proxyFactory.isInitialized(t) ) return
                if ( !childType.isInstance(t) ) return
            }
            EntityReflector target = session.mappingContext.getEntityReflector(association.getAssociatedEntity())

            if (association.isBidirectional()) {
                if (association instanceof ManyToMany) {
                    Collection coll = (Collection) target.getProperty(t, association.getReferencedPropertyName());
                    coll.add(parentAccess.entity);
                } else {
                    target.setProperty(t, association.getReferencedPropertyName(), parentAccess.entity);
                }
            }


            def identifier = target.getIdentifier(t)
            if (identifier == null) { // non-persistent instance
                identifier = session.persist(t);
            }

            if (!reversed && identifier != null) {
                // not sure this is needed but will keep the code
               //session.addPendingRelationshipInsert((Serializable)parentAccess.getIdentifier(), association, OrientGormHelper.createRecordId(identifier))
            }
        }

    }


}
