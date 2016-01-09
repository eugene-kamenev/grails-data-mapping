package org.grails.datastore.gorm.orientdb.mapping.config

import groovy.transform.CompileStatic

@CompileStatic
class OrientConfig {
    String cluster
    String type = 'document'
}
