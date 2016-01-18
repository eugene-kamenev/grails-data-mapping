package org.grails.datastore.gorm.orientdb

import grails.gorm.tests.orientdb.document.*
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite)
@Suite.SuiteClasses([
        //  ValidationSpec,
//  GroovyProxySpec,
//  CommonTypesPersistenceSpec,
//  SaveAllSpec,
//  DomainEventsSpec,
//  ProxyLoadingSpec,
//  QueryAfterPropertyChangeSpec,
//  CircularOneToManySpec,
//  InheritanceSpec,
//  ListOrderBySpec,
//  CriteriaBuilderSpec,
//  NamedQuerySpec,
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
        OrientDbNegationSpec
])
class OrientDbSuite {
}
