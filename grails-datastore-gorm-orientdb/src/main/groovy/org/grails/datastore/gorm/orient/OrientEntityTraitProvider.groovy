package org.grails.datastore.gorm.orient

import grails.orient.OrientEntity
import groovy.transform.CompileStatic
import org.grails.compiler.gorm.GormEntityTraitProvider;

@CompileStatic
class OrientEntityTraitProvider implements GormEntityTraitProvider {
    final Class entityTrait = OrientEntity
}