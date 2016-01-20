package grails.gorm.tests.orientdb.graph

import grails.gorm.tests.GormDatastoreSpec
import org.grails.datastore.gorm.orient.entity.graph.Person

class OrientDbQueryByNullSpec extends GormDatastoreSpec {

    void 'Test passing null as the sole argument to a dynamic finder multiple times'() {
        // see GRAILS-3463
        when:
            def people = Person.findAllByLastName(null)

        then:
            !people

        when:
            people - Person.findAllByLastName(null)

       then:
            !people
    }
}
