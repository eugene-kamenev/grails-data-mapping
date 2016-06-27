package org.grails.datastore.gorm.orient

import grails.orient.OrientEntity
import groovy.transform.CompileStatic
import org.grails.compiler.gorm.GormEntityTraitProvider;

/**
 * OrientEntityTraitProvider implementation
 *
 * @author eugenekamenev
 */
@CompileStatic
class OrientEntityTraitProvider implements GormEntityTraitProvider {
    final Class entityTrait = OrientEntity

    @Override
    boolean isAvailable() {
        return true //TODO: check this
    }
}