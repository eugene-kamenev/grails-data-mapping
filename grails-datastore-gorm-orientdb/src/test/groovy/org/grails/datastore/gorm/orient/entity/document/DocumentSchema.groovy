package org.grails.datastore.gorm.orient.entity.document

import com.orientechnologies.orient.core.id.ORecordId
import grails.gorm.dirty.checking.DirtyCheck
import grails.persistence.*
import groovy.transform.EqualsAndHashCode
import org.grails.datastore.gorm.query.transform.ApplyDetachedCriteriaTransform

@Entity
class Pet implements Serializable {
    static mapWith = 'orient'

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

    static mapWith = 'orient'

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

    static mapWith = 'orient'

    private static final long serialVersionUID = 1

    ORecordId id
    Long version
    String name

    static belongsTo = Pet
}

@Entity
class Parent implements Serializable {
    static mapWith = 'orient'

    private static final long serialVersionUID = 1

    ORecordId id
    String name
    Set<Child> children = []
    static hasMany = [children: Child]
}

@Entity
class Child implements Serializable {
    static mapWith = 'orient'

    private static final long serialVersionUID = 1

    ORecordId id
    Long version
    String name
}

@Entity
class TestEntity implements Serializable {

    static mapWith = 'orient'

    ORecordId id
    Long version
    String name
    Integer age = 30

    ChildEntity child

    static mapping = {
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
class ChildEntity implements Serializable {

    static mapWith = 'orient'

    ORecordId id
    Long version
    String name

    static mapping = {
        name index:true
    }

    static belongsTo = [TestEntity]
}

@Entity
class Face implements Serializable {

    static mapWith = 'orient'

    ORecordId id
    Long version
    String name
    Nose nose
    Person person
    static hasOne = [nose: Nose]
    static belongsTo = [person: Person]

    static constraints = {
        person nullable:true
    }
}

@Entity
class Nose implements Serializable {
    static mapWith = 'orient'

    ORecordId id
    Long version
    boolean hasFreckles
    Face face
    static belongsTo = [face: Face]

    static mapping = {
        face index:true
    }
}

@Entity
class Highway implements Serializable {
    static mapWith = 'orient'

    ORecordId id
    Long version
    Boolean bypassed
    String name

    static mapping = {
        bypassed index:true
        name index:true
    }
}

@Entity
class Book implements Serializable {
    static mapWith = 'orient'

    ORecordId id
    Long version
    String author
    String title
    Boolean published = false

    static mapping = {
        published index:true
        title index:true
        author index:true
    }
}

@Entity
class Location implements Serializable {
    static mapWith = 'orient'

    ORecordId id
    Long version
    String name
    String code = "DEFAULT"

    def namedAndCode() {
        "$name - $code"
    }

    static mapping = {
        name index:true
        code index:true
    }
}

@Entity
class City extends Location {
    static mapWith = 'orient'

    BigDecimal latitude
    BigDecimal longitude
}

@Entity
class Country extends Location {
    static mapWith = 'orient'

    Integer population = 0

    static hasMany = [residents:Person]

    Set residents
}

@Entity
class PlantCategory implements Serializable {
    static mapWith = 'orient'

    ORecordId id
    Long version
    Set plants
    String name

    static hasMany = [plants:Plant]

    static namedQueries = {
//        withPlantsInPatch {
//            plants {
//                eq 'goesInPatch', true
//            }
//        }
//        withPlantsThatStartWithG {
//            plants {
//                like 'name', 'G%'
//            }
//        }
//        withPlantsInPatchThatStartWithG {
//            withPlantsInPatch()
//            withPlantsThatStartWithG()
//        }
    }

    static mapping = {
    }
}

@Entity

class Plant implements Serializable {
    static mapWith = 'orient'

    ORecordId id
    Long version
    boolean goesInPatch
    String name

    static mapping = {
        name index:true
        goesInPatch index:true
    }
}

@Entity
class Publication implements Serializable {
    static mapWith = 'orient'

    ORecordId id
    Long version
    String title
    Date datePublished
    Boolean paperback = true

    static mapping = {
        title index:true
        paperback index:true
        datePublished index:true
    }

    static namedQueries = {

        lastPublishedBefore { date ->
            uniqueResult = true
            le 'datePublished', date
            order 'datePublished', 'desc'
        }

        recentPublications {
            def now = new Date()
            gt 'datePublished', now - 365
        }

        publicationsWithBookInTitle {
            like 'title', 'Book%'
        }

        recentPublicationsByTitle { title ->
            recentPublications()
            eq 'title', title
        }

        latestBooks {
            maxResults(10)
            order("datePublished", "desc")
        }

        publishedBetween { start, end ->
            between 'datePublished', start, end
        }

        publishedAfter { date ->
            gt 'datePublished', date
        }

        paperbackOrRecent {
            or {
                def now = new Date()
                gt 'datePublished', now - 365
                paperbacks()
            }
        }

        paperbacks {
            eq 'paperback', true
        }

        paperbackAndRecent {
            paperbacks()
            recentPublications()
        }

        thisWeeksPaperbacks() {
            paperbacks()
            def today = new Date()
            publishedBetween(today - 7, today)
        }
    }
}