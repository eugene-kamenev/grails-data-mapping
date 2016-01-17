package org.grails.datastore.gorm.orientdb.graph

import com.orientechnologies.orient.core.id.ORecordId
import grails.compiler.GrailsCompileStatic
import grails.gorm.dirty.checking.DirtyCheck
import grails.persistence.Entity
import groovy.transform.EqualsAndHashCode
import org.grails.datastore.gorm.orientdb.ast.OrientEntity
import org.grails.datastore.gorm.query.transform.ApplyDetachedCriteriaTransform

@Entity
@OrientEntity
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
        orient type: 'vertex'
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
@GrailsCompileStatic
@OrientEntity
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
        orient type: 'vertex'
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
@OrientEntity
class PetType implements Serializable {
    private static final long serialVersionUID = 1
    ORecordId id
    Long version
    String name

    static belongsTo = Pet

    static mapping = {
        orient type: 'vertex'
    }
}

@Entity
@OrientEntity
class Parent implements Serializable {
    private static final long serialVersionUID = 1
    ORecordId id
    String name
    Set<Child> children = []
    static hasMany = [children: Child]

    static mapping = {
        orient type: 'vertex'
    }
}

@Entity
@OrientEntity
class Child implements Serializable {
    private static final long serialVersionUID = 1
    ORecordId id
    Long version
    String name

    static mapping = {
        orient type: 'vertex'
    }
}

@Entity
@OrientEntity
class TestEntity implements Serializable {
    ORecordId id
    Long version
    String name
    Integer age = 30

    ChildEntity child

    static mapping = {
        orient type: 'vertex'
        name index:true
        age index:true, nullable:true
        child index:true, nullable:true
    }

    static constraints = {
        name blank:false
        child nullable:true
    }
}

@Entity
@OrientEntity
class ChildEntity implements Serializable {
    ORecordId id
    Long version
    String name

    static mapping = {
        orient type: 'vertex'
        name index:true
    }

    static belongsTo = [TestEntity]
}

@Entity
@OrientEntity
class Face implements Serializable {
    ORecordId id
    Long version
    String name
    Nose nose
    Person person
    static hasOne = [nose: Nose]
    static belongsTo = [person: Person]

    static constraints = {
        orient type: 'vertex'
        person nullable:true
    }
}

@Entity
@OrientEntity
class Nose implements Serializable {
    ORecordId id
    Long version
    boolean hasFreckles
    Face face
    static belongsTo = [face: Face]

    static mapping = {
        orient type: 'vertex'
        face index:true
    }
}

@Entity
@OrientEntity
class Highway implements Serializable {
    ORecordId id
    Long version
    Boolean bypassed
    String name

    static mapping = {
        orient type: 'vertex'
        bypassed index:true
        name index:true
    }
}

@Entity
@OrientEntity
class Book implements Serializable {
    ORecordId id
    Long version
    String author
    String title
    Boolean published = false

    static mapping = {
        orient type: 'vertex'
        published index:true
        title index:true
        author index:true
    }
}

