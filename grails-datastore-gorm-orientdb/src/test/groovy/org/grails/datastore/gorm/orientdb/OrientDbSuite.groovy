package org.grails.datastore.gorm.orientdb

import grails.gorm.tests.orientdb.document.*
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
])
class OrientDbSuite {
}
