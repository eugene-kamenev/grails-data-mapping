package org.grails.datastore.gorm.orientdb

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.orientdb.mapping.config.OrientAttribute
import org.grails.datastore.gorm.orientdb.mapping.config.OrientDbEntity
import org.grails.datastore.mapping.config.AbstractGormMappingFactory
import org.grails.datastore.mapping.config.Property
import org.grails.datastore.mapping.model.ClassMapping
import org.grails.datastore.mapping.model.IdentityMapping
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.types.Identity

import java.beans.PropertyDescriptor

@CompileStatic
class OrientDbGormMappingFactory extends AbstractGormMappingFactory {
    @Override
    protected Class getPropertyMappedFormType() {
        OrientAttribute
    }

    @Override
    protected Class getEntityMappedFormType() {
        OrientDbEntity
    }

    @Override
    IdentityMapping createIdentityMapping(ClassMapping classMapping) {
        return new IdentityMapping() {

            public String[] getIdentifierName() {
                return [IDENTITY_PROPERTY] as String[]
            }

            public ClassMapping getClassMapping() {
                return classMapping;
            }

            public Property getMappedForm() {
                // no custom mapping
                return null;
            }


        };
    }

    @Override
    Identity createIdentity(PersistentEntity owner, MappingContext context, PropertyDescriptor pd) {
        return super.createIdentity(owner, context, pd)
    }
}
