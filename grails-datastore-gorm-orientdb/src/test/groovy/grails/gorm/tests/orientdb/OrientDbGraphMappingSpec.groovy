package grails.gorm.tests.orientdb

import grails.gorm.tests.GormDatastoreSpec
import grails.gorm.tests.PersonGraph

class OrientDbGraphMappingSpec extends GormDatastoreSpec {
    @Override
    List getDomainClasses() {
        [PersonGraph, CityGraph, Profile]
    }

    def "should persist some entities"() {
        when:
            def personOne = new PersonGraph(firstName: "someName", lastName: "someLastName").save(flush: true)
            def personTwo = new PersonGraph(firstName: "someName1", lastName: "someLastName1").save(flush: true)
        then:
            session.clear()
            PersonGraph.count() == 2
    }
}
