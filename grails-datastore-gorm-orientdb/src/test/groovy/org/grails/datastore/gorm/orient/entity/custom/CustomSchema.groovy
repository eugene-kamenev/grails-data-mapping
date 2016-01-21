package org.grails.datastore.gorm.orient.entity.custom

import com.orientechnologies.orient.core.id.ORecordId
import grails.persistence.Entity

@Entity
class Person {
    ORecordId id
    Country livesIn

    static belongsTo = [Country]

    static mapping = {
        orient type: 'vertex'
        livesIn edge: LivesIn
    }
}

@Entity
class Country {
    ORecordId id
    String name
    Integer population = 0
    Set<Person> residents
    Set<City> cities

    static hasMany = [citizens: Person, cities: City]

}

@Entity
class City {
    ORecordId id
    String name
}

@Entity
class LivesIn {
    ORecordId id
    Date since

    static belongsTo = [Person, Country]

    static mapping = {
        orient type: 'edge'
    }
}