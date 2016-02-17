package org.grails.datastore.gorm.orient.entity.custom

import com.orientechnologies.orient.core.id.ORecordId
import grails.persistence.Entity
import org.grails.datastore.gorm.orient.mapping.EdgeType

@Entity
class Person {
    ORecordId id

    Country livesIn

    String firstName
    String lastName

    Set<Person> friends

    static hasMany = [friends: Person]

    static belongsTo = [Country]

    static mappedBy = [friends: 'friends']

    static mapping = {
        orient type: 'vertex'
        livesIn edge: LivesIn
        friends edge: HasFriend
    }
}

@Entity
class Country {
    ORecordId id
    String name
    Integer population = 0
    Set<Person> residents
    Set<City> cities

    static hasMany = [residents: Person, cities: City]

    static mapping = {
        orient type: 'vertex'
        residents edge: LivesIn
    }
}

@Entity
class City {
    ORecordId id
    String name
}
/**
 * OneToMany edge
 */
@Entity
class LivesIn extends EdgeType<Person, Country> {
    Date since

    static mapping = {
    }
}
/**
 * ManyToMany edge
 */
@Entity
class HasFriend extends EdgeType<Person, Person> {
}


