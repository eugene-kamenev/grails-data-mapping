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

@CompileStatic
class OrientEdgeAssociationQueryExecutor implements AssociationQueryExecutor<OIdentifiable, Object>  {
    private final OIdentifiable identifiable
    private final Association association
    private final OrientSession session

    OrientEdgeAssociationQueryExecutor(Association association, OrientSession session, OIdentifiable identifiable = null) {
        this.association = association
        this.session = session
        this.identifiable = identifiable
    }

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

    @Override
    PersistentEntity getIndexedEntity() {
        return association.associatedEntity
    }

    @Override
    boolean doesReturnKeys() {
        return false
    }
}
