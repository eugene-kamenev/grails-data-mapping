package org.grails.datastore.gorm.orient.entity.custom

import com.orientechnologies.orient.core.id.ORecordId
import grails.persistence.Entity

@Entity
class Person {
    ORecordId id

    Country livesIn

    String firstName
    String lastName

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

@Entity
class LivesIn extends OrientEdge<Person, Country> {
    Date since

    static mapping = {
        orient type: 'edge'
    }
}

@Entity
class OrientEdge<T, E> {
    ORecordId id

    private T inVertex
    private E outVertex

    static belongsTo = [T, E]

    void setIn(T instance) {
        inVertex = instance
    }

    T getIn() {
        inVertex
    }

    E getOut() {
        outVertex
    }

    void setOut(E instance) {
        this.outVertex = instance
    }
}
