package grails.orient

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.GormEntity

@CompileStatic
trait OrientEntity<D> extends GormEntity<D> {
}