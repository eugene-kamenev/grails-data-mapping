package org.grails.datastore.gorm.orientdb

import grails.gorm.tests.orientdb.document.OrientDbDocumentCriteriaBuilderSpec
import grails.gorm.tests.orientdb.document.OrientDbDocumentDetachedCriteriaSpec
import grails.gorm.tests.orientdb.document.OrientDbDocumentGormEnhancerSpec
import grails.gorm.tests.orientdb.document.OrientDbDocumentOneToOneSpec
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
        OrientDbDocumentDetachedCriteriaSpec, OrientDbDocumentGormEnhancerSpec, OrientDbDocumentCriteriaBuilderSpec, OrientDbDocumentOneToOneSpec
])
class OrientDbSuite {
}
