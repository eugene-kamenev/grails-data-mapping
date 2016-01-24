package org.grails.datastore.gorm.orient.mapping.config

import groovy.transform.CompileStatic

/**
 * Defines main mapping option
 *
 * @author eugenekamenev
 */
@CompileStatic
class OrientConfig {
    String cluster
    String type = 'document'
}
