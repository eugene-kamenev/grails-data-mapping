package org.grails.datastore.gorm.orient.engine

import com.orientechnologies.orient.core.db.record.OIdentifiable
import com.orientechnologies.orient.core.record.impl.ODocument
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.orient.OrientSession
import org.grails.datastore.gorm.orient.extensions.OrientGormHelper
import org.grails.datastore.gorm.orient.mapping.RelationshipUtils
import org.grails.datastore.gorm.orient.mapping.config.OrientAttribute
import org.grails.datastore.mapping.core.impl.PendingInsertAdapter
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.model.types.*

/**
 * Association insert that will persist relationships
 */
@CompileStatic
class OrientPendingRelationshipInsert extends PendingInsertAdapter<Object, Serializable> {
    protected final OrientSession session
    protected final Collection<Serializable> pendingInserts
    protected final boolean isUpdate
    protected final Association association

    OrientPendingRelationshipInsert(EntityAccess parent, Association association, Collection<Serializable> pendingInserts, OrientSession session, boolean isUpdate = false) {
        super(parent.persistentEntity, (Serializable) parent.identifier, parent.entity, parent)
        this.pendingInserts = pendingInserts
        this.isUpdate = isUpdate
        this.association = association
        this.session = session
    }

    @Override
    void run() {
        boolean reversed = RelationshipUtils.useReversedMappingFor(association);
        def associationMapping = (OrientAttribute) association.mapping.mappedForm;
        ODocument target = (ODocument)((OIdentifiable) nativeKey).record
        if (!reversed && (association instanceof ToOne)) {
            if (association instanceof ManyToOne && !association.isOwningSide()) {
                OrientPersistentPropertyConverter.setValue(target, association, pendingInserts[0], associationMapping.type)
            }
        }
        if (association instanceof OneToMany && !association.isOwningSide()) {
            def oType = OrientGormHelper.getOTypeForField(association)
            OrientPersistentPropertyConverter.setValue(target, association, pendingInserts, oType)
        }
        if (association instanceof OneToOne && association.isOwningSide()) {
            def oType = OrientGormHelper.getOTypeForField(association)
            OrientPersistentPropertyConverter.setValue(target, association, pendingInserts[0], oType)
        }
    }
}
