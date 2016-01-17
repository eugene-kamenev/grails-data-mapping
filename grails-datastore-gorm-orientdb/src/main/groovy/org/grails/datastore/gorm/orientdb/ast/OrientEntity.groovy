package org.grails.datastore.gorm.orientdb.ast

import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Mark entity as Orient one, will apply AST Transformation
 */
@Target([ElementType.TYPE])
@Retention(RetentionPolicy.SOURCE)
@GroovyASTTransformationClass(['org.grails.datastore.gorm.orientdb.ast.OrientTransformation'])
@interface OrientEntity {
}