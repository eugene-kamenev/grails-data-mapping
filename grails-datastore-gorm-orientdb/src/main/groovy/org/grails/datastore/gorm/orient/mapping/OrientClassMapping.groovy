package org.grails.datastore.gorm.orient.mapping

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.orient.OrientPersistentEntity
import org.grails.datastore.gorm.orient.mapping.config.OrientDbEntity
import org.grails.datastore.mapping.model.AbstractClassMapping
import org.grails.datastore.mapping.model.IdentityMapping
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity

@CompileStatic
class OrientClassMapping extends AbstractClassMapping<OrientDbEntity> {

    OrientClassMapping(PersistentEntity entity, MappingContext context) {
        super(entity, context)
    }

    @Override
    OrientDbEntity getMappedForm() {
        return ((OrientPersistentEntity)entity).mappedForm
    }

    @Override
    IdentityMapping getIdentifier() {
        return super.getIdentifier()
    }
}
