package org.grails.datastore.gorm.orient

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.orient.mapping.OrientClassMapping
import org.grails.datastore.gorm.orient.mapping.config.OrientEntity
import org.grails.datastore.mapping.model.AbstractPersistentEntity
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentProperty

/**
 * OrientDB Persistent Entity implementation
 *
 * @author eugenekamenev
 */
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

    /**
     * If entity mapped as document
     *
     * @return
     */
    boolean isDocument() {
        mappedForm.orient.type == 'document'
    }

    /**
     * If entity mapped as vertex
     *
     * @return
     */
    boolean isVertex() {
        mappedForm.orient.type == 'vertex'
    }

    /**
     * If entity mapped as edge
     * @return
     */
    boolean isEdge() {
        mappedForm.orient.type == 'edge'
    }

    /**
     * If entity mapped as graph one
     *
     * @return
     */
    boolean isGraph() {
        edge || vertex
    }

    /**
     * Get mapped class name
     *
     * @return
     */
    String getClassName() {
        if (!orientClassName) {
            orientClassName = mappedForm.orient.cluster ?: javaClass.simpleName
        }
        orientClassName
    }

    /**
     * Identity getter
     *
     * @return
     */
    @Override
    PersistentProperty getIdentity() {
        return super.getIdentity()
    }

    /**
     * Get mapped entity property name
     *
     * @param name
     * @return
     */
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
