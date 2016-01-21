package grails.gorm.tests.orientdb.custom

import grails.gorm.tests.GormDatastoreSpec
import org.grails.datastore.gorm.orient.entity.custom.Country
import org.grails.datastore.gorm.orient.entity.custom.Person

class OrientMixedRelationshipsSpec extends GormDatastoreSpec {

    def "test that entity relationship is saved with edge"() {
        given:
        def p
        when:
        p = new Person(livesIn: new Country()).save(flush: true).id
        then:
        p != null
    }

    def "test that entity relationship saved from inverse side" () {
        given:
            def c, p

        when:
            p = new Person()
            c = new Country(residents: [p]).save(flush: true)
        then:
            p.livesIn != null
            c.residents.size() == 1
    }
}
