package org.grails.datastore.gorm.orientdb.document

import com.orientechnologies.orient.core.id.ORecordId
import grails.gorm.dirty.checking.DirtyCheck
import grails.gorm.tests.Face
import grails.persistence.Entity
import groovy.transform.EqualsAndHashCode
import org.grails.datastore.gorm.query.transform.ApplyDetachedCriteriaTransform

@Entity
class Pet implements Serializable {
    ORecordId id
    Long version
    String name
    Date birthDate = new Date()
    PetType type = new PetType(name:"Unknown")
    Person owner
    Integer age
    Face face

    static mapping = {
        name index:true
    }

    static constraints = {
        owner nullable:true
        age nullable: true
        face nullable:true
    }
}

@DirtyCheck
@Entity
@ApplyDetachedCriteriaTransform
//@groovy.transform.EqualsAndHashCode - breaks gorm-neo4j: TODO: http://jira.grails.org/browse/GPNEO4J-10
@EqualsAndHashCode(includes = ['firstName', 'lastName', 'age'])
class Person implements Serializable, Comparable<Person> {
    static simpsons = where {
        lastName == "Simpson"
    }

    ORecordId id
    Long version
    String firstName
    String lastName
    Integer age = 0
    Set<Pet> pets = [] as Set
    static hasMany = [pets:Pet]
    Face face
    boolean myBooleanProperty

//    static peopleWithOlderPets = where {
//        pets {
//            age > 9
//        }
//    }
//    static peopleWithOlderPets2 = where {
//        pets.age > 9
//    }

    static Person getByFirstNameAndLastNameAndAge(String firstName, String lastName, int age) {
        find( new Person(firstName: firstName, lastName: lastName, age: age) )
    }

    static mapping = {
        firstName index:true, attr: 'first__name'
        lastName index:true, attr: 'last__name'
        age index:true
    }

    static constraints = {
        face nullable:true
    }

    @Override
    int compareTo(Person t) {
        age <=> t.age
    }
}

@Entity
class PetType implements Serializable {
    private static final long serialVersionUID = 1
    ORecordId id
    Long version
    String name

    static belongsTo = Pet
}

@Entity
class Parent implements Serializable {
    private static final long serialVersionUID = 1
    ORecordId id
    String name
    Set<Child> children = []
    static hasMany = [children: Child]
}

@Entity
class Child implements Serializable {
    private static final long serialVersionUID = 1
    ORecordId id
    Long version
    String name
}