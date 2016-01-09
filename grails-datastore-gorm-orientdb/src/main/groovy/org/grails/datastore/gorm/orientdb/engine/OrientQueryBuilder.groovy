package org.grails.datastore.gorm.orientdb.engine

import static com.github.raymanrt.orientqb.query.ProjectionFunction.*
import static com.github.raymanrt.orientqb.query.Projection.*
import com.github.raymanrt.orientqb.query.Query
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.orientdb.OrientDbPersistentEntity
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.query.AssociationQuery
import org.grails.datastore.mapping.query.Query as GrailsQuery

@CompileStatic
class OrientQueryBuilder extends Query {
    protected final OrientDbPersistentEntity entity
    
    OrientQueryBuilder(OrientDbPersistentEntity entity) {
        this.entity = entity
        from(entity.className)
    }
    
    Query build(GrailsQuery.ProjectionList projectionList, GrailsQuery.Junction criterion, Map queryArgs) {
        applyProjections(projectionList, queryArgs)
        applyCriterions(criterion, queryArgs)
        if (queryArgs.max) {
            this.limit(queryArgs.max as int)
        }
        if (queryArgs.offset) {
            this.skip(queryArgs.offset as int)
        }
        if (queryArgs.sort) {
            this.orderBy(projection(queryArgs.sort as String))
        }
        this
    }

    Query applyProjections(GrailsQuery.ProjectionList projections, Map queryArgs) {
        for(projection in projections.projectionList) {
            def handler = PROJECT_HANDLERS.get(projection.class)
            handler?.handle(entity, projection, this)
        }
        this
    }
    
    Query applyCriterions(GrailsQuery.Junction junction, Map queryArgs) {
        for(criterion in junction.criteria) {
            def handler = CRITERION_HANDLERS.get(criterion.class)
            handler?.handle(entity, criterion, this)
        }
        this
    }

    protected static Map<Class<? extends GrailsQuery.Projection>, ProjectionHandler> PROJECT_HANDLERS = [
            (GrailsQuery.CountProjection)        : new ProjectionHandler<GrailsQuery.CountProjection>() {
                @Override
                @CompileStatic
                def handle(OrientDbPersistentEntity entity, GrailsQuery.CountProjection countProjection, Query query) {
                    query.select(count(projection("*")))
                }
            },
            (GrailsQuery.CountDistinctProjection): new ProjectionHandler<GrailsQuery.CountDistinctProjection>() {
                @Override
                @CompileStatic
                def handle(OrientDbPersistentEntity entity, GrailsQuery.CountDistinctProjection countDistinctProjection, Query query) {
                    query.select(count(distinct(projection(countDistinctProjection.propertyName))))
                }
            },
            (GrailsQuery.MinProjection)          : new ProjectionHandler<GrailsQuery.MinProjection>() {
                @Override
                @CompileStatic
                def handle(OrientDbPersistentEntity entity, GrailsQuery.MinProjection minProjection, Query query) {
                    query.select(min(projection(minProjection.propertyName)))
                }
            },
            (GrailsQuery.MaxProjection)          : new ProjectionHandler<GrailsQuery.MaxProjection>() {
                @Override
                @CompileStatic
                def handle(OrientDbPersistentEntity entity, GrailsQuery.MaxProjection maxProjection, Query query) {
                    query.select(max(projection(maxProjection.propertyName)))
                }
            },
            (GrailsQuery.SumProjection)          : new ProjectionHandler<GrailsQuery.SumProjection>() {
                @Override
                @CompileStatic
                def handle(OrientDbPersistentEntity entity, GrailsQuery.SumProjection sumProjection, Query query) {
                    query.select(sum(projection(sumProjection.propertyName)))
                }
            },
            (GrailsQuery.AvgProjection)          : new ProjectionHandler<GrailsQuery.AvgProjection>() {
                @Override
                @CompileStatic
                def handle(OrientDbPersistentEntity entity, GrailsQuery.AvgProjection avgProjection, Query query) {
                    query.select(avg(projection(avgProjection.propertyName)))
                }
            },
            (GrailsQuery.PropertyProjection)     : new ProjectionHandler<GrailsQuery.PropertyProjection>() {
                @Override
                @CompileStatic
                def handle(OrientDbPersistentEntity entity, GrailsQuery.PropertyProjection projection, Query query) {
                    def propertyName = ((GrailsQuery.PropertyProjection) projection).propertyName
                    def association = entity.getPropertyByName(propertyName)
                    if (association instanceof Association) {
                        /*def targetNodeName = "${association.name}_${builder.getNextMatchNumber()}"
                        builder.addMatch("(n)${matchForAssociation(association)}(${targetNodeName})")*/
                        return ""
//                        return targetNodeName
                    } else {
                        return query.select(association.name)
                    }
                }
            }
    ]

    public static Map<Class<? extends GrailsQuery.Criterion>, CriterionHandler> CRITERION_HANDLERS = [
            (GrailsQuery.Conjunction)              : new CriterionHandler<GrailsQuery.Conjunction>() {
                @Override
                @CompileStatic
                def handle(OrientDbPersistentEntity entity, GrailsQuery.Conjunction criterion, Query query) {
                    def inner = ((GrailsQuery.Junction) criterion).criteria
                            .collect {GrailsQuery.Criterion it ->
                        def handler = CRITERION_HANDLERS.get(it.getClass())
                        if (handler == null) {
                            throw new UnsupportedOperationException("Criterion of type ${it.class.name} are not supported by GORM for Neo4j")
                        }
                        handler.handle(entity, it, null).toString()
                    }
                    .join(CriterionHandler.OPERATOR_AND)
                }
            },
            (GrailsQuery.Disjunction)              : new CriterionHandler<GrailsQuery.Disjunction>() {
                @Override
                @CompileStatic
                def handle(OrientDbPersistentEntity entity, GrailsQuery.Disjunction criterion, Query query) {
                    def inner = ((GrailsQuery.Junction) criterion).criteria
                            .collect {GrailsQuery.Criterion it ->
                        def handler = CRITERION_HANDLERS.get(it.getClass())
                        if (handler == null) {
                            throw new UnsupportedOperationException("Criterion of type ${it.class.name} are not supported by GORM for Neo4j")
                        }
                        handler.handle(entity, it, query).toString()
                    }
                    .join(CriterionHandler.OPERATOR_OR)
                }
            },
            (GrailsQuery.Negation)                 : new CriterionHandler<GrailsQuery.Negation>() {
                @Override
                @CompileStatic
                def handle(OrientDbPersistentEntity entity, GrailsQuery.Negation criterion, Query query) {
                    List<GrailsQuery.Criterion> criteria = criterion.criteria
                    def disjunction = new GrailsQuery.Disjunction(criteria)
                    CriterionHandler<GrailsQuery.Disjunction> handler = {->
                        CRITERION_HANDLERS.get(GrailsQuery.Disjunction)
                    }.call()
                    //new OrientSQLExpression("NOT (${handler.handle(entity, disjunction, builder, query)})")
                }
            },
            (GrailsQuery.Equals)                   : new CriterionHandler<GrailsQuery.Equals>() {
                @Override
                @CompileStatic
                def handle(OrientDbPersistentEntity entity, GrailsQuery.Equals criterion, Query query) {
                    /*Neo4jMappingContext mappingContext = (Neo4jMappingContext)entity.mappingContext
                    int paramNumber = builder.addParam( mappingContext.convertToNative(criterion.value) )
                    def association = entity.getPropertyByName(criterion.property)
                    String lhs
                    if (association instanceof Association) {
                        def targetNodeName = "m_${builder.getNextMatchNumber()}"
                        builder.addMatch("(${query})${matchForAssociation(association)}(${targetNodeName})")

                        def graphEntity = (GraphPersistentEntity) ((Association) association).associatedEntity
                        if(graphEntity.idGenerator == null) {
                            lhs = "ID(${targetNodeName})"
                        }
                        else {
                            lhs = "${targetNodeName}.${CypherBuilder.IDENTIFIER}"
                        }
                    } else {
                        def graphEntity = (GraphPersistentEntity) entity
                        if(graphEntity.idGenerator == null) {
                            lhs = criterion.property == "id" ? "ID(${query})" : "${query}.${criterion.property}"
                        }
                        else {
                            lhs = criterion.property == "id" ? "${query}.${CypherBuilder.IDENTIFIER}" : "${query}.${criterion.property}"
                        }
                    }

                    return new OrientSQLExpression(lhs, "{$paramNumber}", CriterionHandler.OPERATOR_EQUALS)*/
                    def association = entity.getPropertyByName(criterion.property)
                    if (association instanceof Association) {

                    } else {
                        query.where(projection(criterion.property).eq(criterion.value))
                    }
                }
            },
            (GrailsQuery.IdEquals)                 : new CriterionHandler<GrailsQuery.IdEquals>() {
                @Override
                @CompileStatic
                def handle(OrientDbPersistentEntity entity, GrailsQuery.IdEquals criterion, Query query) {
                    /*int paramNumber = addBuildParameterForCriterion(builder, entity, criterion)
                    GraphPersistentEntity graphPersistentEntity = (GraphPersistentEntity)entity
                    if(graphPersistentEntity.idGenerator == null) {
                        return new OrientSQLExpression(ID_EQUALS, "{$paramNumber}", CriterionHandler.OPERATOR_EQUALS)
                    }
                    else {
                        return new OrientSQLExpression("${query}.${CypherBuilder.IDENTIFIER}", "{$paramNumber}", CriterionHandler.OPERATOR_EQUALS)
                    }*/
                }
            },
            (GrailsQuery.Like)                     : new CriterionHandler<GrailsQuery.Like>() {
                @Override
                @CompileStatic
                def handle(OrientDbPersistentEntity entity, GrailsQuery.Like criterion, Query query) {
                    query.where(projection(criterion.property).like(criterion.value))
                }
            },
            (GrailsQuery.ILike)                    : new CriterionHandler<GrailsQuery.ILike>() {
                @Override
                @CompileStatic
                def handle(OrientDbPersistentEntity entity, GrailsQuery.ILike criterion, Query query) {
                    query.where(projection(criterion.property).like(criterion.value))
                }
            },
            (GrailsQuery.RLike)                    : new CriterionHandler<GrailsQuery.RLike>() {
                @Override
                @CompileStatic
                def handle(OrientDbPersistentEntity entity, GrailsQuery.RLike criterion, Query query) {
                    query.where(projection(criterion.property).like(criterion.value))
                }
            },
            (GrailsQuery.In)                       : new CriterionHandler<GrailsQuery.In>() {
                @Override
                @CompileStatic
                def handle(OrientDbPersistentEntity entity, GrailsQuery.In criterion, Query query) {
                    /*    int paramNumber = addBuildParameterForCriterion(builder, entity, criterion)
                        GraphPersistentEntity graphPersistentEntity = (GraphPersistentEntity)entity
                        String lhs
                        if(graphPersistentEntity.idGenerator == null) {
                            lhs = criterion.property == "id" ? "ID(${query})" : "${query}.$criterion.property"
                        }
                        else {
                            lhs = criterion.property == "id" ? "${query}.${CypherBuilder.IDENTIFIER}" : "${query}.$criterion.property"
                        }
                        builder.replaceParamAt(paramNumber, convertEnumsInList(((Query.In) criterion).values))
                        return new OrientSQLExpression(lhs, "{$paramNumber}", CriterionHandler.OPERATOR_IN)*/
                }
            },
            (GrailsQuery.IsNull)                   : new CriterionHandler<GrailsQuery.IsNull>() {
                @Override
                @CompileStatic
                def handle(OrientDbPersistentEntity entity, GrailsQuery.IsNull criterion, Query query) {
                }
            },
            (AssociationQuery)                                                  : new AssociationQueryHandler(),
            (GrailsQuery.GreaterThan)              : ComparisonCriterionHandler.GREATER_THAN,
            (GrailsQuery.GreaterThanEquals)        : ComparisonCriterionHandler.GREATER_THAN_EQUALS,
            (GrailsQuery.LessThan)                 : ComparisonCriterionHandler.LESS_THAN,
            (GrailsQuery.LessThanEquals)           : ComparisonCriterionHandler.LESS_THAN_EQUALS,
            (GrailsQuery.NotEquals)                : ComparisonCriterionHandler.NOT_EQUALS,

            (GrailsQuery.GreaterThanProperty)      : PropertyComparisonCriterionHandler.GREATER_THAN,
            (GrailsQuery.GreaterThanEqualsProperty): PropertyComparisonCriterionHandler.GREATER_THAN_EQUALS,
            (GrailsQuery.LessThanProperty)         : PropertyComparisonCriterionHandler.LESS_THAN,
            (GrailsQuery.LessThanEqualsProperty)   : PropertyComparisonCriterionHandler.LESS_THAN_EQUALS,
            (GrailsQuery.NotEqualsProperty)        : PropertyComparisonCriterionHandler.NOT_EQUALS,
            (GrailsQuery.EqualsProperty)           : PropertyComparisonCriterionHandler.EQUALS,

            (GrailsQuery.Between)                  : new CriterionHandler<GrailsQuery.Between>() {
                @Override
                @CompileStatic
                def handle(OrientDbPersistentEntity entity, GrailsQuery.Between criterion, Query query) {
                    /* int paramNumber = addBuildParameterForCriterion(builder, entity, criterion)
                     Neo4jMappingContext mappingContext = (Neo4jMappingContext)entity.mappingContext
                     int paramNumberFrom = builder.addParam( mappingContext.convertToNative(criterion.from) )
                     int parmaNumberTo = builder.addParam( mappingContext.convertToNative(criterion.to) )
                     new OrientSQLExpression( "{$paramNumberFrom}<=${query}.$criterion.property and ${query}.$criterion.property<={$parmaNumberTo}")*/
                }
            },
            (GrailsQuery.SizeLessThanEquals)       : SizeCriterionHandler.LESS_THAN_EQUALS,
            (GrailsQuery.SizeLessThan)             : SizeCriterionHandler.LESS_THAN,
            (GrailsQuery.SizeEquals)               : SizeCriterionHandler.EQUALS,
            (GrailsQuery.SizeNotEquals)            : SizeCriterionHandler.NOT_EQUALS,
            (GrailsQuery.SizeGreaterThan)          : SizeCriterionHandler.GREATER_THAN,
            (GrailsQuery.SizeGreaterThanEquals)    : SizeCriterionHandler.GREATER_THAN_EQUALS

    ]

    /**
     * Interface for handling projections when building Cypher queries
     *
     * @param < T >  The projection type
     */
    static interface ProjectionHandler<T extends GrailsQuery.Projection> {
        String COUNT = "count(*)"

        def handle(OrientDbPersistentEntity entity, T projection, Query query)
    }

    /**
     * Interface for handling criterion when building Cypher queries
     *
     * @param < T >  The criterion type
     */
    static interface CriterionHandler<T extends GrailsQuery.Criterion> {
        String COUNT = "count"
        String OPERATOR_EQUALS = '='
        String OPERATOR_NOT_EQUALS = '<>'
        String OPERATOR_LIKE = "like"
        String OPERATOR_IN = ""
        String OPERATOR_AND = "AND"
        String OPERATOR_OR = "OR"
        String OPERATOR_GREATER_THAN = ">"
        String OPERATOR_LESS_THAN = "<"
        String OPERATOR_GREATER_THAN_EQUALS = ">="
        String OPERATOR_LESS_THAN_EQUALS = "<="
        String OPERATOR_RANGE = "BETWEEN"

        def handle(OrientDbPersistentEntity entity, T criterion, Query query)
    }


    /**
     * Handles AssociationQuery instances
     */
    @CompileStatic
    static class AssociationQueryHandler implements CriterionHandler<AssociationQuery> {
        @Override
        def handle(OrientDbPersistentEntity entity, AssociationQuery criterion, Query query) {
            AssociationQuery aq = criterion as AssociationQuery
            // def targetNodeName = "m_${builder.getNextMatchNumber()}"
            //  builder.addMatch("(n)${matchForAssociation(aq.association)}(${targetNodeName})")
            //  def s = CRITERION_HANDLERS.get(aq.criteria.getClass()).handle(entity, aq.criteria, targetNodeName)
            def s = CRITERION_HANDLERS.get(aq.criteria.getClass()).handle(entity, aq.criteria, null)
            //return new OrientSQLExpression(s)
            null
        }
    }

    /**
     * A criterion handler for comparison criterion
     *
     * @param < T >
     */
    @CompileStatic
    static class ComparisonCriterionHandler<T extends GrailsQuery.PropertyCriterion> implements CriterionHandler<T> {
        public static
        final ComparisonCriterionHandler<GrailsQuery.GreaterThanEquals> GREATER_THAN_EQUALS = new ComparisonCriterionHandler<GrailsQuery.GreaterThanEquals>(CriterionHandler.OPERATOR_GREATER_THAN_EQUALS)
        public static
        final ComparisonCriterionHandler<GrailsQuery.GreaterThan> GREATER_THAN = new ComparisonCriterionHandler<GrailsQuery.GreaterThan>(CriterionHandler.OPERATOR_GREATER_THAN)
        public static
        final ComparisonCriterionHandler<GrailsQuery.LessThan> LESS_THAN = new ComparisonCriterionHandler<GrailsQuery.LessThan>(CriterionHandler.OPERATOR_LESS_THAN)
        public static
        final ComparisonCriterionHandler<GrailsQuery.LessThanEquals> LESS_THAN_EQUALS = new ComparisonCriterionHandler<GrailsQuery.LessThanEquals>(CriterionHandler.OPERATOR_LESS_THAN_EQUALS)
        public static
        final ComparisonCriterionHandler<GrailsQuery.NotEquals> NOT_EQUALS = new ComparisonCriterionHandler<GrailsQuery.NotEquals>(CriterionHandler.OPERATOR_NOT_EQUALS)
        public static
        final ComparisonCriterionHandler<GrailsQuery.Equals> EQUALS = new ComparisonCriterionHandler<GrailsQuery.Equals>(CriterionHandler.OPERATOR_EQUALS)

        final String operator

        ComparisonCriterionHandler(String operator) {
            this.operator = operator
        }

        @Override
        def handle(OrientDbPersistentEntity entity, T criterion, Query query) {
            /*int paramNumber = addBuildParameterForCriterion(builder, entity, criterion)
            String lhs = "${query}.${criterion.property}"
            return new OrientSQLExpression(lhs, "{$paramNumber}", operator)*/
        }
    }

    /**
     * A criterion handler for comparison criterion
     *
     * @param < T >
     */
    @CompileStatic
    static class PropertyComparisonCriterionHandler<T extends GrailsQuery.PropertyComparisonCriterion> implements CriterionHandler<T> {
        public static
        final PropertyComparisonCriterionHandler<GrailsQuery.GreaterThanEqualsProperty> GREATER_THAN_EQUALS = new PropertyComparisonCriterionHandler<GrailsQuery.GreaterThanEqualsProperty>(CriterionHandler.OPERATOR_GREATER_THAN_EQUALS)
        public static
        final PropertyComparisonCriterionHandler<GrailsQuery.GreaterThanProperty> GREATER_THAN = new PropertyComparisonCriterionHandler<GrailsQuery.GreaterThanProperty>(CriterionHandler.OPERATOR_GREATER_THAN)
        public static
        final PropertyComparisonCriterionHandler<GrailsQuery.LessThanProperty> LESS_THAN = new PropertyComparisonCriterionHandler<GrailsQuery.LessThanProperty>(CriterionHandler.OPERATOR_LESS_THAN)
        public static
        final PropertyComparisonCriterionHandler<GrailsQuery.LessThanEqualsProperty> LESS_THAN_EQUALS = new PropertyComparisonCriterionHandler<GrailsQuery.LessThanEqualsProperty>(CriterionHandler.OPERATOR_LESS_THAN_EQUALS)
        public static
        final PropertyComparisonCriterionHandler<GrailsQuery.NotEqualsProperty> NOT_EQUALS = new PropertyComparisonCriterionHandler<GrailsQuery.NotEqualsProperty>(CriterionHandler.OPERATOR_NOT_EQUALS)
        public static
        final PropertyComparisonCriterionHandler<GrailsQuery.EqualsProperty> EQUALS = new PropertyComparisonCriterionHandler<GrailsQuery.EqualsProperty>(CriterionHandler.OPERATOR_EQUALS)

        final String operator

        PropertyComparisonCriterionHandler(String operator) {
            this.operator = operator
        }

        @Override
        def handle(OrientDbPersistentEntity entity, T criterion, Query query) {
/*
            def operator = COMPARISON_OPERATORS.get(criterion.getClass())
            if (operator == null) {
                throw new UnsupportedOperationException("Unsupported Neo4j property comparison: ${criterion}")
            }
            return new OrientSQLExpression("$query.${criterion.property}${operator}n.${criterion.otherProperty}")
*/
        }
    }
    /**
     * A citerion handler for size related queries
     *
     * @param < T >
     */
    @CompileStatic
    static class SizeCriterionHandler<T extends GrailsQuery.PropertyCriterion> implements CriterionHandler<T> {

        public static
        final SizeCriterionHandler<GrailsQuery.SizeEquals> EQUALS = new SizeCriterionHandler<GrailsQuery.SizeEquals>(CriterionHandler.OPERATOR_EQUALS)
        public static
        final SizeCriterionHandler<GrailsQuery.SizeNotEquals> NOT_EQUALS = new SizeCriterionHandler<GrailsQuery.SizeNotEquals>(CriterionHandler.OPERATOR_NOT_EQUALS)
        public static
        final SizeCriterionHandler<GrailsQuery.SizeGreaterThan> GREATER_THAN = new SizeCriterionHandler<GrailsQuery.SizeGreaterThan>(CriterionHandler.OPERATOR_GREATER_THAN)
        public static
        final SizeCriterionHandler<GrailsQuery.SizeGreaterThanEquals> GREATER_THAN_EQUALS = new SizeCriterionHandler<GrailsQuery.SizeGreaterThanEquals>(CriterionHandler.OPERATOR_GREATER_THAN_EQUALS)
        public static
        final SizeCriterionHandler<GrailsQuery.SizeLessThan> LESS_THAN = new SizeCriterionHandler<GrailsQuery.SizeLessThan>(CriterionHandler.OPERATOR_LESS_THAN)
        public static
        final SizeCriterionHandler<GrailsQuery.SizeLessThanEquals> LESS_THAN_EQUALS = new SizeCriterionHandler<GrailsQuery.SizeLessThanEquals>(CriterionHandler.OPERATOR_LESS_THAN_EQUALS)

        final String operator;

        SizeCriterionHandler(String operator) {
            this.operator = operator
        }

        @Override
        def handle(OrientDbPersistentEntity entity, T criterion, Query query) {
            //int paramNumber = addBuildParameterForCriterion(builder, entity, criterion)
            Association association = entity.getPropertyByName(criterion.property) as Association
            //builder.addMatch("(${query})${matchForAssociation(association)}() WITH ${query},count(*) as count")
//            return new OrientSQLExpression(CriterionHandler.COUNT, "{}", operator)
        }
    }
} 
