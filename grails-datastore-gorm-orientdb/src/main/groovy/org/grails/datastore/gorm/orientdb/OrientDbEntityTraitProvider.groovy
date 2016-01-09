package org.grails.datastore.gorm.orientdb

import grails.orientdb.OrientDbEntity
import groovy.transform.CompileStatic
import org.grails.compiler.gorm.GormEntityTraitProvider;

@CompileStatic
class OrientDbEntityTraitProvider implements GormEntityTraitProvider {
    final Class entityTrait = OrientDbEntity
}