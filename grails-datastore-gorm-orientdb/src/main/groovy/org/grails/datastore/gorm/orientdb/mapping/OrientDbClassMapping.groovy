package org.grails.datastore.gorm.orientdb.mapping

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.orientdb.OrientDbPersistentEntity
import org.grails.datastore.gorm.orientdb.mapping.config.OrientDbEntity
import org.grails.datastore.mapping.model.AbstractClassMapping
import org.grails.datastore.mapping.model.IdentityMapping
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity

@CompileStatic
class OrientDbClassMapping extends AbstractClassMapping<OrientDbEntity> {

    OrientDbClassMapping(PersistentEntity entity, MappingContext context) {
        super(entity, context)
    }

    @Override
    OrientDbEntity getMappedForm() {
        return ((OrientDbPersistentEntity)entity).mappedForm
    }

    @Override
    IdentityMapping getIdentifier() {
        return super.getIdentifier()
    }
}
