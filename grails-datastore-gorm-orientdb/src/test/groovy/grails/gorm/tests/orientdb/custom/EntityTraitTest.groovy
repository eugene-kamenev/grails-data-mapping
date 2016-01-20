package grails.gorm.tests.orientdb.custom

import grails.orient.OrientEntity
import spock.lang.Specification

class EntityTraitTest extends Specification {

    def "Test GORM adds OrientEntity"() {
            when:
            def cls = new GroovyClassLoader().parseClass('''
import grails.persistence.*

@Entity
class Foo {
    static mapWith = 'orient'

    String name
}
''')
            then:
            OrientEntity.isAssignableFrom(cls)

    }
}
