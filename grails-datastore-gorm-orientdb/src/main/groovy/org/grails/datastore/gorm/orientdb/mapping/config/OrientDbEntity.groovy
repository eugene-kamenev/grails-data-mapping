package org.grails.datastore.gorm.orientdb.mapping.config

import org.grails.datastore.mapping.config.Entity

class OrientDbEntity extends Entity {
    final boolean versioned = false
    final boolean autoTimestamp = false
    OrientConfig orient = new OrientConfig()
}
