package grails.orient

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.GormEntity
/**
 * OrientDB entity trait for handling special methods
 * TODO: add methods for dynamic properties handling
 * TODO: add methods for getting native instances Vertex or Edge for custom
 * gremlin pipe
 *
 * @param <D>
 */
@CompileStatic
trait OrientEntity<D> extends GormEntity<D> {


    def methodMissing(String name, def args) {

    }

    def propertyMissing(String name) {
        return null
    }

    def propertyMissing(String name, def arg) {

    }

    def out(String name) {

    }

    def 'in'(String name) {

    }
}