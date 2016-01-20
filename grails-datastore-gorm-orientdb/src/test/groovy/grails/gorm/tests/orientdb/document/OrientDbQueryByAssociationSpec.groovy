package grails.gorm.tests.orientdb.document

import grails.gorm.tests.GormDatastoreSpec
import org.grails.datastore.gorm.orient.entity.document.ChildEntity
import org.grails.datastore.gorm.orient.entity.document.TestEntity

/**
 * Abstract base test for query associations. Subclasses should do the necessary setup to configure GORM
 */
class OrientDbQueryByAssociationSpec extends GormDatastoreSpec {

    void "Test query entity by single-ended association"() {
        given:
            def age = 40
            ["Bob", "Fred", "Barney", "Frank"].each { new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save() }

        when:
            def child = ChildEntity.findByName("Barney Child")

        then:
            child != null
            child.id != null

        when:
            def t = TestEntity.findByChild(child)

        then:
            t != null
            "Barney" == t.name
    }
}
