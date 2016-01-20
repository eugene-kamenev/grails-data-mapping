package org.grails.datastore.gorm.orient.mapping.config

import org.grails.datastore.mapping.config.Entity

class OrientEntity extends Entity {
    final boolean versioned = false
    final boolean autoTimestamp = false
    OrientConfig orient = new OrientConfig()
}
