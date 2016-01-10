package org.grails.datastore.gorm.orientdb

import grails.gorm.tests.orientdb.graph.OrientDbGraphCriteriaBuilderSpec
import grails.gorm.tests.orientdb.graph.OrientDbGraphDetachedCriteriaSpec
import grails.gorm.tests.orientdb.graph.OrientDbGraphGormEnhancerSpec
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
//  FindByMethodSpec,
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
        OrientDbGraphDetachedCriteriaSpec, OrientDbGraphGormEnhancerSpec, OrientDbGraphCriteriaBuilderSpec
])
class OrientDbSuite {
}
