package org.grails.datastore.gorm.orient.mapping.config

import groovy.transform.CompileStatic

@CompileStatic
class OrientConfig {
    String cluster
    String type = 'document'
}
