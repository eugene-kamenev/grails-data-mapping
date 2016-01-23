package grails.orient

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.GormEntity

@CompileStatic
trait OrientEntity<D> extends GormEntity<D> {

    def methodMissing(String name, def args) {

    }

    def propertyMissing(String name) {
        return null
    }

    def propertyMissing(String name, def arg) {

    }
}