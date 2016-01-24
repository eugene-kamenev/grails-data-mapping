package org.grails.datastore.gorm.orient.mapping

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.orient.OrientPersistentEntity
import org.grails.datastore.gorm.orient.mapping.config.OrientEntity
import org.grails.datastore.mapping.model.AbstractClassMapping
import org.grails.datastore.mapping.model.IdentityMapping
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity

/**
 * OrientClassMapping implementation
 *
 * @author eugenekamenev
 */
@CompileStatic
class OrientClassMapping extends AbstractClassMapping<OrientEntity> {

    OrientClassMapping(PersistentEntity entity, MappingContext context) {
        super(entity, context)
    }

    @Override
    OrientEntity getMappedForm() {
        return ((OrientPersistentEntity)entity).mappedForm
    }

    @Override
    IdentityMapping getIdentifier() {
        return super.getIdentifier()
    }
}
