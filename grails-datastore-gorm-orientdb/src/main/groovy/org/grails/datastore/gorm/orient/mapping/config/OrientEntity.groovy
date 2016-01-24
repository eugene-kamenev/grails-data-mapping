package org.grails.datastore.gorm.orient.mapping.config

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.config.Entity

/**
 * Orient GORM Entity implementation
 *
 * @author eugenekamenev
 */
@CompileStatic
class OrientEntity extends Entity {
    final boolean versioned = false
    final boolean autoTimestamp = false
    OrientConfig orient = new OrientConfig()
}
