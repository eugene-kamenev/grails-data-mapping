package org.grails.datastore.gorm.orient

import grails.gorm.tests.orientdb.graph.*
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite)
@Suite.SuiteClasses([
        OrientDbCriteriaBuilderSpec,
        OrientDbCrudOperationsSpec,
        OrientDbDeleteAllSpec,
        OrientDbDetachedCriteriaSpec,
        OrientDbFindByMethodSpec,
        OrientDbFindWhereSpec,
        OrientDbGormEnhancerSpec,
        OrientDbGroovyProxySpec,
        OrientDbNamedQuerySpec,
        OrientDbNegationSpec,
        OrientDbOneToManySpec,
        OrientDbOneToOneSpec,
        OrientDbPagedResultSpec,
        OrientDbQueryByAssociationSpec,
        OrientDbQueryByNullSpec,
        OrientDbRangeQuerySpec,
        OrientDbSaveAllSpec,
        OrientDbSizeQuerySpec,
        OrientDbWithTransactionSpec
//OrientMixedRelationshipsSpec
])
class OrientDbSuite {
}
