package grails.orientdb

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.GormEntity

@CompileStatic
trait OrientDbEntity<D> extends GormEntity<D> {
}