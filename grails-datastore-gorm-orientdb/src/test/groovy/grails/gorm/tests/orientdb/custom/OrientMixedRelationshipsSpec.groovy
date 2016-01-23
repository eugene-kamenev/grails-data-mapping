package grails.gorm.tests.orientdb.custom

import grails.gorm.tests.GormDatastoreSpec
import org.grails.datastore.gorm.orient.entity.custom.Country
import org.grails.datastore.gorm.orient.entity.custom.LivesIn
import org.grails.datastore.gorm.orient.entity.custom.Person

class OrientMixedRelationshipsSpec extends GormDatastoreSpec {

    def "test that entity relationship saved from edge side"() {
        given:
            def person = new Person(firstName: 'Homer', lastName: 'Simpson')
            def country = new Country(name: 'England')
            def date = new Date(1990, 10, 12)
            def livesIn = new LivesIn(in: person, out: country, since: date).save(flush: true)
            session.clear()
        when:
            Person p = Person.get(person.id)
        then:
            p.livesIn != null
        and:
            p.livesIn.name == 'England'
        and:
            p.livesIn.residents.size() == 1
            p.livesIn.residents[0].lastName == 'Simpson'
        when:
            LivesIn edge = LivesIn.get(livesIn.id)
        then:
            edge.in.firstName == 'Homer'
            edge.out.cities.size() == 0
            edge.since == date
    }

    def "test that entity relationship saved from owner side" () {
        given:
            def person = new Person(firstName: 'Homer', lastName: 'Simpson', livesIn: new Country(name: 'England')).save(flush: true)
            session.clear()
        when:
            Person p = Person.get(person.id)
        then:
            p.livesIn.name == 'England'
            p.livesIn.residents.size() == 1
            p.livesIn.residents[0].firstName == 'Homer'
        when:
            Country c = Country.get(p.livesIn.id)
        then:
            c.name == 'England'
            c.residents[0].firstName == 'Homer'



    }
}
