package org.grails.datastore.gorm.orientdb.ast

import com.orientechnologies.orient.core.db.record.OIdentifiable
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.EmptyExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.tools.GeneralUtils
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

/**
 * AST Transformation will inject 'dbInstance' field and class-constructor
 */
@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class OrientTransformation extends AbstractASTTransformation {

    static final ClassNode orientNode = ClassHelper.make(OIdentifiable).plainNodeReference

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        // Vertex annotation node
        AnnotationNode annotation = (AnnotationNode) nodes[0];
        // current transformation class node
        ClassNode annotatedClass = (ClassNode) nodes[1];
        def dbInstaceFieldNode = annotatedClass.addField('dbInstance', ACC_PUBLIC, orientNode, new EmptyExpression())
        def constructorBlock = new BlockStatement()
        def constructorParam = GeneralUtils.param(orientNode, "_dbInstance")
        constructorBlock.addStatement(GeneralUtils.assignS(GeneralUtils.varX(dbInstaceFieldNode), GeneralUtils.varX(constructorParam)))
        annotatedClass.addConstructor(new ConstructorNode(ACC_PUBLIC, new BlockStatement()))
        annotatedClass.addConstructor(ACC_PUBLIC, [constructorParam] as Parameter[], [] as ClassNode[], constructorBlock)
    }
}
