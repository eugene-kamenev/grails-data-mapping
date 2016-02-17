package org.grails.datastore.gorm.orient.engine

import com.orientechnologies.orient.core.db.record.OIdentifiable
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.orient.OrientPersistentEntity
import org.grails.datastore.gorm.orient.OrientSession
import org.grails.datastore.gorm.orient.collection.OrientResultList
import org.grails.datastore.gorm.orient.extensions.OrientExtensions
import org.grails.datastore.gorm.orient.mapping.config.OrientAttribute
import org.grails.datastore.mapping.engine.AssociationQueryExecutor
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.ManyToMany

/**
 * OrientDB simple edge association query via Gremlin Pipe executor
 *
 * @author eugenekamenev
 */
@CompileStatic
class OrientEdgeAssociationQueryExecutor implements AssociationQueryExecutor<OIdentifiable, Object>  {
    private final OIdentifiable identifiable
    private final Association association
    private final OrientSession session
    private final boolean isLazy

    OrientEdgeAssociationQueryExecutor(Association association, OrientSession session, boolean isLazy, OIdentifiable identifiable = null) {
        this.association = association
        this.session = session
        this.identifiable = identifiable
        this.isLazy = isLazy
    }

    /**
     * Here is a special logic about primaryKey, actually it
     * will be vertex one as a start for a Gremlin Pipe
     *
     * @param primaryKey The primary key
     * @return
     */
    @Override
    List<Object> query(OIdentifiable primaryKey) {
        def key = primaryKey
        if (!key) {
            key = identifiable
        }
        def mapping = (OrientAttribute) association.mapping.mappedForm
        if (mapping.edge != null) {
            def edgeAssociationEntity = (OrientPersistentEntity) session.mappingContext.getPersistentEntity(mapping.edge.name)
            def inAssociation = (Association) edgeAssociationEntity.getPropertyByName('in')
            def outAssociation = (Association) edgeAssociationEntity.getPropertyByName('out')
            def edgeName = edgeAssociationEntity.className
            def result = []
            if (association instanceof ManyToMany) {
                return new OrientResultList(0, (Iterator) OrientExtensions.pipe(session.graph.getVertex(key)).both(edgeName).iterator(), (OrientEntityPersister) session.getPersister(association.associatedEntity))
            }
            if (inAssociation.associatedEntity != association.owner) {
                return new OrientResultList(0, (Iterator) OrientExtensions.pipe(session.graph.getVertex(key)).out(edgeName).iterator(), (OrientEntityPersister) session.getPersister(association.associatedEntity))
            }
            if (outAssociation.associatedEntity != association.owner) {
                return new OrientResultList(0, (Iterator) OrientExtensions.pipe(session.graph.getVertex(key)).in(edgeName).iterator(), (OrientEntityPersister) session.getPersister(association.associatedEntity))
            }

        }
        if (mapping.type in OrientPersistentPropertyConverter.linkedTypes) {
            return null
        }
    }

    /**
     * Get associated persistent entity
     * @return
     */
    @Override
    PersistentEntity getIndexedEntity() {
        return association.associatedEntity
    }

    /**
     * Query does not return keys
     * @return
     */
    @Override
    boolean doesReturnKeys() {
        return false
    }
}
