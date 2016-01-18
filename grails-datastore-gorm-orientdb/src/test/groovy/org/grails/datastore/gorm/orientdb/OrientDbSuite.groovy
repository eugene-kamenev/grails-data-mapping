package org.grails.datastore.gorm.orientdb

import grails.gorm.tests.orientdb.document.*
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite)
@Suite.SuiteClasses([
        //  ValidationSpec,
//  GroovyProxySpec,
 //CommonTypesPersistenceSpec,
//  DomainEventsSpec,
//  ProxyLoadingSpec,
//  QueryAfterPropertyChangeSpec,
//  CircularOneToManySpec,
//  InheritanceSpec,
//  ListOrderBySpec,
//  OrderBySpec,
//  RangeQuerySpec,
//  UpdateWithProxyPresentSpec,
//  AttachMethodSpec,
//  WithTransactionSpec,
//  SaveAllSpec,
//  DeleteAllSpec
//SizeQuerySpec
        OrientDbDetachedCriteriaSpec,
        OrientDbGormEnhancerSpec,
        OrientDbCriteriaBuilderSpec,
        OrientDbOneToOneSpec,
        OrientDbFindByMethodSpec,
        OrientDbOneToManySpec,
        OrientDbFindWhereSpec,
        OrientDbCrudOperationsSpec,
        OrientDbWithTransactionSpec,
        OrientDbNegationSpec,
        OrientDbNamedQuerySpec
])
class OrientDbSuite {
}
