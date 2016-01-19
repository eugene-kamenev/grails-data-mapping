package org.grails.datastore.gorm.orientdb.mapping.config

import com.orientechnologies.orient.core.metadata.schema.OClass
import com.orientechnologies.orient.core.metadata.schema.OType
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.document.config.Attribute

@CompileStatic
class OrientAttribute extends Attribute {
    String field
    OType type
    OClass.INDEX_TYPE index

    void setField(String field) {
        setTargetName(field)
        this.field = field
    }
}
