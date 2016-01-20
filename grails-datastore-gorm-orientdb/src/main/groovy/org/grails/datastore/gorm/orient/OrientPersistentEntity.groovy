package org.grails.datastore.gorm.orient

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.orient.mapping.OrientClassMapping
import org.grails.datastore.gorm.orient.mapping.config.OrientEntity
import org.grails.datastore.mapping.model.AbstractPersistentEntity
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentProperty

@CompileStatic
class OrientPersistentEntity extends AbstractPersistentEntity<OrientEntity> {

    protected final OrientEntity mappedForm
    protected final OrientClassMapping classMapping
    protected String orientClassName

    OrientPersistentEntity(Class javaClass, MappingContext context) {
        this(javaClass, context, false)
    }

    OrientPersistentEntity(Class javaClass, MappingContext context, boolean external) {
        super(javaClass, context)
        if(isExternal()) {
            this.mappedForm = null;
        }
        else {
            this.mappedForm = (OrientEntity) context.getMappingFactory().createMappedForm(this);
        }
        this.external = external
        this.classMapping = new OrientClassMapping(this, context)
    }

    OrientEntity getMappedForm() {
        mappedForm
    }

    boolean isDocument() {
        mappedForm.orient.type == 'document'
    }

    boolean isVertex() {
        mappedForm.orient.type == 'vertex'
    }

    boolean isEdge() {
        mappedForm.orient.type == 'edge'
    }

    boolean isGraph() {
        edge || vertex
    }

    OrientClassMapping getClassMapping() {
        classMapping
    }

    String getClassName() {
        if (!orientClassName) {
            orientClassName = mappedForm.orient.cluster ?: javaClass.simpleName
        }
        orientClassName
    }

    @Override
    PersistentProperty getIdentity() {
        return super.getIdentity()
    }

    String getNativePropertyName(String name) {
        if (identity.name == name) {
            return "@rid"
        }
        def propName = getPropertyByName(name).mapping.mappedForm.targetName
        if (!propName) {
            propName = name
        }
        propName
    }
}
