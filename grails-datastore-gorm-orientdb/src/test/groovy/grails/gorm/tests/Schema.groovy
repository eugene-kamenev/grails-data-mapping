package grails.gorm.tests

import com.orientechnologies.orient.core.metadata.schema.OType
import grails.persistence.Entity

@Entity
class PersonGraph {
    String firstName
    String lastName
    CityGraph livesIn
    List<CityGraph> visitedCities
    CityGraph cityGraphLink
    List<CityGraph> cityGraphLinkedList
    Set<CityGraph> cityGraphLinkedSet
    List<CityGraph> notVisitedCities
    Profile profile

    static belongsTo = CityGraph

    static hasMany = [visitedCities: CityGraph]

    static mapping = {
        orient type: 'vertex', cluster: 'Person'
        firstName(index: 'unique')
        profile(type: OType.EMBEDDED)
        livesIn(edge: Lives)
        visitedCities(edge: Visited)
        cityGraphLink(type: OType.LINK)
        cityGraphLinkedList(type: OType.LINKLIST)
        cityGraphLinkedSet(type: OType.LINKSET)
//        notVisitedCities(formula: 'select from CityGraph where @rid not in (?)', params: this.getVisitedCities())
    }
}

@Entity
class Profile {
    String telephone
}

@Entity
class Visited {
}

@Entity
class Lives {
    Date since
}

@Entity
class CityGraph {
    String title
    Date dateCreated

    List<PersonGraph> visitedPersonGraphs
    List<PersonGraph> citizens

    static hasMany = [visitedPersonGraphs: PersonGraph, citizens: PersonGraph]

    static mapping = {
        dateCreated(field: 'date_created')
        visitedPersonGraphs(edge: Visited)
        citizens(edge: Lives)
    }
}
