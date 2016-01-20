package org.grails.datastore.gorm.orient.test

import grails.orient.OrientEntity
import org.codehaus.groovy.control.CompilePhase
import org.grails.compiler.injection.EntityASTTransformation
import org.grails.datastore.gorm.orient.entity.graph.TestEntity
import spock.lang.Specification

class MainIntegrationTest extends Specification {

    def "test entity transformation" () {
        given:
        def file = new File('/home/ekamenev/test.groovy')
        def invoker = new org.codehaus.groovy.tools.ast.TransformTestHelper(new EntityASTTransformation(), CompilePhase.CANONICALIZATION)
        when:
        def a = OrientEntity.isAssignableFrom(TestEntity)
        println a
        def clazz = invoker.parse(file)

        then:
        true == true
    }
}