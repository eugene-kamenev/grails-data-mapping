package grails.gorm.tests.orientdb.custom

import grails.gorm.tests.GormDatastoreSpec
import org.grails.datastore.gorm.orient.entity.custom.Country
import org.grails.datastore.gorm.orient.entity.custom.Person

class OrientMixedRelationshipsSpec extends GormDatastoreSpec {

    def "test that entity relationship is saved with edge"() {
        given:

        def c = new Country(name: 'England')
        def p = new Person(livesIn: c).save(flush: true)

        session.clear()

        when:
            p = Person.get(p.id)
        then:
            p != null
        and:
            p.country != null
            p.country.name == 'England'


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
