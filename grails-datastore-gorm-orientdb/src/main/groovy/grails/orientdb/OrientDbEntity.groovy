package grails.orientdb

import com.orientechnologies.orient.core.id.ORecordId
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.GormEntity

@CompileStatic
trait OrientDbEntity<D> extends GormEntity<D> {
    ORecordId id
}