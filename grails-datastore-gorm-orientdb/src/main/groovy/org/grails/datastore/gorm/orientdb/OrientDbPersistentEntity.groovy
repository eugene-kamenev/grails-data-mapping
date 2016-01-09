package org.grails.datastore.gorm.orientdb

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.orientdb.mapping.OrientDbClassMapping
import org.grails.datastore.gorm.orientdb.mapping.config.OrientDbEntity
import org.grails.datastore.mapping.model.AbstractPersistentEntity
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentProperty

@CompileStatic
class OrientDbPersistentEntity extends AbstractPersistentEntity<OrientDbEntity> {

    protected final OrientDbEntity mappedForm
    protected final OrientDbClassMapping classMapping
    protected String orientClassName

    OrientDbPersistentEntity(Class javaClass, MappingContext context) {
        this(javaClass, context, false)
    }

    OrientDbPersistentEntity(Class javaClass, MappingContext context, boolean external) {
        super(javaClass, context)
        if(isExternal()) {
            this.mappedForm = null;
        }
        else {
            this.mappedForm = (OrientDbEntity) context.getMappingFactory().createMappedForm(this);
        }
        this.external = external
        this.classMapping = new OrientDbClassMapping(this, context)
    }

    OrientDbEntity getMappedForm() {
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

    OrientDbClassMapping getClassMapping() {
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
        def propName = getPropertyByName(name).mapping.mappedForm.targetName
        if (!propName) {
            propName = name
        }
        propName
    }
}
