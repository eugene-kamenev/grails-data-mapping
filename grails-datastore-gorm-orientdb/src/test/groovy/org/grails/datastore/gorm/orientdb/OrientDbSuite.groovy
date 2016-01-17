package org.grails.datastore.gorm.orientdb

import grails.gorm.tests.orientdb.graph.*
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite)
@Suite.SuiteClasses([
        //  ValidationSpec,
//  GroovyProxySpec,
//  CommonTypesPersistenceSpec,
//  OneToManySpec
//  SaveAllSpec,
//  GormEnhancerSpec,
//  DomainEventsSpec,
//  ProxyLoadingSpec,
//  QueryAfterPropertyChangeSpec,
//  CircularOneToManySpec,
//  InheritanceSpec,
//  ListOrderBySpec,
//  CriteriaBuilderSpec,
//  NegationSpec,
//  NamedQuerySpec,
//  OrderBySpec,
//  RangeQuerySpec,
//  UpdateWithProxyPresentSpec,
//  AttachMethodSpec,
//  WithTransactionSpec,
//  CrudOperationsSpec,
//  SaveAllSpec,
//  DeleteAllSpec
//    OneToOneSpec
//    FindWhereSpec
//DetachedCriteriaSpec
//SizeQuerySpec
  //      OneToManySpec,
        OrientDbDetachedCriteriaSpec,
        OrientDbGormEnhancerSpec,
        OrientDbCriteriaBuilderSpec,
        OrientDbOneToOneSpec,
        OrientDbFindByMethodSpec,
        OrientDbOneToManySpec
])
class OrientDbSuite {
}
