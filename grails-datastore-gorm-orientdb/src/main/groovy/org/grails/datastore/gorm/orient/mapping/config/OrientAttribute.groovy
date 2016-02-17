package org.grails.datastore.gorm.orient.mapping.config

import com.orientechnologies.orient.core.metadata.schema.OClass
import com.orientechnologies.orient.core.metadata.schema.OType
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.document.config.Attribute
/**
 * OrientAttribute for mapping
 *
 * @author eugenekamenev
 */
@CompileStatic
class OrientAttribute extends Attribute {
    String field
    OType type
    OClass.INDEX_TYPE index
    Class edge

    void setField(String field) {
        setTargetName(field)
        this.field = field
    }

    boolean isLinkedCollection() {
        type.isLink()
    }
}
