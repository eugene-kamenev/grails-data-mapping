package grails.orient

import com.orientechnologies.orient.core.db.record.OIdentifiable
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.GormEntity

@CompileStatic
trait OrientEntity<D> extends GormEntity<D> {
    OIdentifiable dbInstance
}