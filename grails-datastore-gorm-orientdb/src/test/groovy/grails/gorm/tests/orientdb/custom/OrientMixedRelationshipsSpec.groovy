package grails.gorm.tests.orientdb.custom

import grails.gorm.tests.GormDatastoreSpec
import org.grails.datastore.gorm.orient.entity.custom.Country
import org.grails.datastore.gorm.orient.entity.custom.LivesIn
import org.grails.datastore.gorm.orient.entity.custom.Person

class OrientMixedRelationshipsSpec extends GormDatastoreSpec {

    def "test that entity relationship saved from edge side"() {
        given:
            def homer = new Person(firstName: 'Homer', lastName: 'Simpson')
            def lisa = new Person(firstName: 'Lisa', lastName: 'Simpson')
            def marge = new Person(firstName: 'Marge', lastName: 'Simpson')
            def england = new Country(name: 'England')
            def usa = new Country(name: 'USA')
            def date = new Date(1990, 10, 12)
            lisa.livesIn = usa
            marge.livesIn = england
            marge.save()
            lisa.save()
            def homerLivesIn = new LivesIn(in: homer, out: england, since: date).save(flush: true)
            session.clear()
        when:
            homer = Person.findByFirstNameAndLastName('Homer', 'Simpson')
            lisa = Person.findByFirstNameLike('Li%')
            marge = Person.findByFirstName('Marge')
        then:
            homer.livesIn != null
            marge.livesIn != null
            lisa.livesIn != null
        and:
            homer.livesIn.name == 'England'
            marge.livesIn.name == 'England'
            lisa.livesIn.name == 'USA'
        and:
            homer.livesIn.residents.size() == 2
            marge.livesIn.residents.size() == 2
            lisa.livesIn.residents.size() == 1
        and:
            marge.livesIn.id == homer.livesIn.id
            lisa.livesIn.residents[0].firstName == 'Lisa'
        when:
            LivesIn edge = LivesIn.get(homerLivesIn.id)
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
