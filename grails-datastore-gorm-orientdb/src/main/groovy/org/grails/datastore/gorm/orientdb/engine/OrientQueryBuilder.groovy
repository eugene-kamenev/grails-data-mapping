package org.grails.datastore.gorm.orientdb.engine

import com.github.raymanrt.orientqb.query.Clause
import com.github.raymanrt.orientqb.query.Operator
import com.github.raymanrt.orientqb.query.Projection
import com.github.raymanrt.orientqb.query.Query
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.orientdb.OrientDbPersistentEntity
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.query.AssociationQuery
import org.grails.datastore.mapping.query.Query as GrailsQuery

import static com.github.raymanrt.orientqb.query.Clause.*
import static com.github.raymanrt.orientqb.query.Projection.projection
import static com.github.raymanrt.orientqb.query.ProjectionFunction.*

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
            limit(queryArgs.max as int)
        }
        if (queryArgs.offset) {
            skip(queryArgs.offset as int)
        }
        if (queryArgs.sort) {
            if (queryArgs.sort instanceof String) {
                orderBy(projection(entity.getNativePropertyName(queryArgs.sort as String)))
            }
            if (queryArgs.sort instanceof Map) {
                for(value in (Map)queryArgs.sort) {
                    def orderProjection = projection(entity.getNativePropertyName(value.key as String))
                    if (value.value == GrailsQuery.Order.Direction.DESC) {
                        orderByDesc(orderProjection)
                    } else {
                        orderBy(orderProjection)
                    }
                }
            }
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
            this.where(handler?.handle(entity, criterion, this))
        }
        this
    }

    protected static Map<Class<? extends GrailsQuery.Projection>, ProjectionHandler> PROJECT_HANDLERS = [
            (GrailsQuery.CountProjection)        : new ProjectionHandler<GrailsQuery.CountProjection>() {
                @Override
                @CompileStatic
                def handle(OrientDbPersistentEntity entity, GrailsQuery.CountProjection countProjection, Query query) {
                    query.select(count(Projection.ALL))
                }
            },
            (GrailsQuery.CountDistinctProjection): new ProjectionHandler<GrailsQuery.CountDistinctProjection>() {
                @Override
                @CompileStatic
                def handle(OrientDbPersistentEntity entity, GrailsQuery.CountDistinctProjection countDistinctProjection, Query query) {
                    query.select(count(distinct(projection(entity.getNativePropertyName(countDistinctProjection.propertyName)))))
                }
            },
            (GrailsQuery.MinProjection)          : new ProjectionHandler<GrailsQuery.MinProjection>() {
                @Override
                @CompileStatic
                def handle(OrientDbPersistentEntity entity, GrailsQuery.MinProjection minProjection, Query query) {
                    query.select(min(projection(entity.getNativePropertyName(minProjection.propertyName))))
                }
            },
            (GrailsQuery.MaxProjection)          : new ProjectionHandler<GrailsQuery.MaxProjection>() {
                @Override
                @CompileStatic
                def handle(OrientDbPersistentEntity entity, GrailsQuery.MaxProjection maxProjection, Query query) {
                    query.select(max(projection(entity.getNativePropertyName(maxProjection.propertyName))))
                }
            },
            (GrailsQuery.SumProjection)          : new ProjectionHandler<GrailsQuery.SumProjection>() {
                @Override
                @CompileStatic
                def handle(OrientDbPersistentEntity entity, GrailsQuery.SumProjection sumProjection, Query query) {
                    query.select(sum(projection(entity.getNativePropertyName(sumProjection.propertyName))))
                }
            },
            (GrailsQuery.AvgProjection)          : new ProjectionHandler<GrailsQuery.AvgProjection>() {
                @Override
                @CompileStatic
                def handle(OrientDbPersistentEntity entity, GrailsQuery.AvgProjection avgProjection, Query query) {
                    query.select(avg(projection(entity.getNativePropertyName(avgProjection.propertyName))))
                }
            },
            (GrailsQuery.PropertyProjection)     : new ProjectionHandler<GrailsQuery.PropertyProjection>() {
                @Override
                @CompileStatic
                def handle(OrientDbPersistentEntity entity, GrailsQuery.PropertyProjection propertyProjection, Query query) {
                    def propertyName = propertyProjection.propertyName
                    def association = entity.getPropertyByName(propertyName)
                    if (association instanceof Association) {
                        query.select(expand(projection(entity.getNativePropertyName(propertyName))))
                    } else {
                        query.select(projection(entity.getNativePropertyName(propertyName)))
                    }
                }
            }
    ]

    public static Map<Class<? extends GrailsQuery.Criterion>, CriterionHandler> CRITERION_HANDLERS = [
            (GrailsQuery.Conjunction)              : new CriterionHandler<GrailsQuery.Conjunction>() {
                @Override
                @CompileStatic
                Clause handle(OrientDbPersistentEntity entity, GrailsQuery.Conjunction criterion, Query query) {
                    def inner = ((GrailsQuery.Junction) criterion).criteria
                            .collect {GrailsQuery.Criterion it ->
                        def handler = CRITERION_HANDLERS.get(it.getClass())
                        if (handler == null) {
                            throw new UnsupportedOperationException("Criterion of type ${it.class.name} are not supported by GORM for OrientDb")
                        }
                        handler.handle(entity, it, query)
                    }
                    and(inner as Clause[])
                }
            },
            (GrailsQuery.Disjunction)              : new CriterionHandler<GrailsQuery.Disjunction>() {
                @Override
                @CompileStatic
                Clause handle(OrientDbPersistentEntity entity, GrailsQuery.Disjunction criterion, Query query) {
                    def inner = ((GrailsQuery.Junction) criterion).criteria.collect { GrailsQuery.Criterion it ->
                        def handler = CRITERION_HANDLERS.get(it.getClass())
                        if (handler == null) {
                            throw new UnsupportedOperationException("Criterion of type ${it.class.name} are not supported by GORM for OrientDb")
                        }
                        handler.handle(entity, it, query)
                    }
                    return or(inner as Clause[])
                }
            },
            (GrailsQuery.Negation)                 : new CriterionHandler<GrailsQuery.Negation>() {
                @Override
                @CompileStatic
                Clause handle(OrientDbPersistentEntity entity, GrailsQuery.Negation criterion, Query query) {
                    List<GrailsQuery.Criterion> criteria = criterion.criteria
                    def disjunction = new GrailsQuery.Disjunction(criteria)
                    CriterionHandler<GrailsQuery.Disjunction> handler = {->
                        CRITERION_HANDLERS.get(GrailsQuery.Disjunction)
                    }.call()
                    null
                }
            },
            (GrailsQuery.Equals)                   : new CriterionHandler<GrailsQuery.Equals>() {
                @Override
                @CompileStatic
                Clause handle(OrientDbPersistentEntity entity, GrailsQuery.Equals criterion, Query query) {
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
                        return clause(projection(entity.getNativePropertyName(criterion.property)), Operator.EQ, criterion.value)
                    }
                    return null
                }
            },
            (GrailsQuery.IdEquals)                 : new CriterionHandler<GrailsQuery.IdEquals>() {
                @Override
                @CompileStatic
                Clause handle(OrientDbPersistentEntity entity, GrailsQuery.IdEquals criterion, Query query) {
                    projection(entity.getNativePropertyName(criterion.property)).eq(criterion.value)
                }
            },
            (GrailsQuery.Like)                     : new CriterionHandler<GrailsQuery.Like>() {
                @Override
                @CompileStatic
                Clause handle(OrientDbPersistentEntity entity, GrailsQuery.Like criterion, Query query) {
                    projection(entity.getNativePropertyName(criterion.property)).like(criterion.value)
                }
            },
            (GrailsQuery.ILike)                    : new CriterionHandler<GrailsQuery.ILike>() {
                @Override
                @CompileStatic
                Clause handle(OrientDbPersistentEntity entity, GrailsQuery.ILike criterion, Query query) {
                    projection(entity.getNativePropertyName(criterion.property)).like(criterion.value)
                }
            },
            (GrailsQuery.RLike)                    : new CriterionHandler<GrailsQuery.RLike>() {
                @Override
                @CompileStatic
                Clause handle(OrientDbPersistentEntity entity, GrailsQuery.RLike criterion, Query query) {
                    projection(entity.getNativePropertyName(criterion.property)).like(criterion.value)
                }
            },
            (GrailsQuery.In)                       : new CriterionHandler<GrailsQuery.In>() {
                @Override
                @CompileStatic
                Clause handle(OrientDbPersistentEntity entity, GrailsQuery.In criterion, Query query) {
                    clause(projection(entity.getNativePropertyName(criterion.property)), Operator.IN, criterion.values)
                }
            },
            (GrailsQuery.IsNull)                   : new CriterionHandler<GrailsQuery.IsNull>() {
                @Override
                @CompileStatic
                Clause handle(OrientDbPersistentEntity entity, GrailsQuery.IsNull criterion, Query query) {
                    projection(entity.getNativePropertyName(criterion.property)).isNull()
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
                Clause handle(OrientDbPersistentEntity entity, GrailsQuery.Between criterion, Query query) {
                    projection(entity.getNativePropertyName(criterion.property)).between((Number)criterion.from, (Number)criterion.to)
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
     * Interface for handling projections when building OrientDb queries
     *
     * @param < T >  The projection type
     */
    static interface ProjectionHandler<T extends GrailsQuery.Projection> {
        def handle(OrientDbPersistentEntity entity, T projection, Query query)
    }

    /**
     * Interface for handling criterion when building OrientDb queries
     *
     * @param < T >  The criterion type
     */
    static interface CriterionHandler<T extends GrailsQuery.Criterion> {
        Clause handle(OrientDbPersistentEntity entity, T criterion, Query query)
    }


    /**
     * Handles AssociationQuery instances
     */
    @CompileStatic
    static class AssociationQueryHandler implements CriterionHandler<AssociationQuery> {
        @Override
        Clause handle(OrientDbPersistentEntity entity, AssociationQuery criterion, Query query) {
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
        final ComparisonCriterionHandler<GrailsQuery.GreaterThanEquals> GREATER_THAN_EQUALS = new ComparisonCriterionHandler<GrailsQuery.GreaterThanEquals>()
        public static
        final ComparisonCriterionHandler<GrailsQuery.GreaterThan> GREATER_THAN = new ComparisonCriterionHandler<GrailsQuery.GreaterThan>()
        public static
        final ComparisonCriterionHandler<GrailsQuery.LessThan> LESS_THAN = new ComparisonCriterionHandler<GrailsQuery.LessThan>()
        public static
        final ComparisonCriterionHandler<GrailsQuery.LessThanEquals> LESS_THAN_EQUALS = new ComparisonCriterionHandler<GrailsQuery.LessThanEquals>()
        public static
        final ComparisonCriterionHandler<GrailsQuery.NotEquals> NOT_EQUALS = new ComparisonCriterionHandler<GrailsQuery.NotEquals>()
        public static
        final ComparisonCriterionHandler<GrailsQuery.Equals> EQUALS = new ComparisonCriterionHandler<GrailsQuery.Equals>()

        ComparisonCriterionHandler() {
        }

        @Override
        Clause handle(OrientDbPersistentEntity entity, T criterion, Query query) {
            null
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
        final PropertyComparisonCriterionHandler<GrailsQuery.GreaterThanEqualsProperty> GREATER_THAN_EQUALS = new PropertyComparisonCriterionHandler<GrailsQuery.GreaterThanEqualsProperty>()
        public static
        final PropertyComparisonCriterionHandler<GrailsQuery.GreaterThanProperty> GREATER_THAN = new PropertyComparisonCriterionHandler<GrailsQuery.GreaterThanProperty>()
        public static
        final PropertyComparisonCriterionHandler<GrailsQuery.LessThanProperty> LESS_THAN = new PropertyComparisonCriterionHandler<GrailsQuery.LessThanProperty>()
        public static
        final PropertyComparisonCriterionHandler<GrailsQuery.LessThanEqualsProperty> LESS_THAN_EQUALS = new PropertyComparisonCriterionHandler<GrailsQuery.LessThanEqualsProperty>()
        public static
        final PropertyComparisonCriterionHandler<GrailsQuery.NotEqualsProperty> NOT_EQUALS = new PropertyComparisonCriterionHandler<GrailsQuery.NotEqualsProperty>()
        public static
        final PropertyComparisonCriterionHandler<GrailsQuery.EqualsProperty> EQUALS = new PropertyComparisonCriterionHandler<GrailsQuery.EqualsProperty>()

        PropertyComparisonCriterionHandler() {
        }

        @Override
        Clause handle(OrientDbPersistentEntity entity, T criterion, Query query) {
            null
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
        final SizeCriterionHandler<GrailsQuery.SizeEquals> EQUALS = new SizeCriterionHandler<GrailsQuery.SizeEquals>()
        public static
        final SizeCriterionHandler<GrailsQuery.SizeNotEquals> NOT_EQUALS = new SizeCriterionHandler<GrailsQuery.SizeNotEquals>()
        public static
        final SizeCriterionHandler<GrailsQuery.SizeGreaterThan> GREATER_THAN = new SizeCriterionHandler<GrailsQuery.SizeGreaterThan>()
        public static
        final SizeCriterionHandler<GrailsQuery.SizeGreaterThanEquals> GREATER_THAN_EQUALS = new SizeCriterionHandler<GrailsQuery.SizeGreaterThanEquals>()
        public static
        final SizeCriterionHandler<GrailsQuery.SizeLessThan> LESS_THAN = new SizeCriterionHandler<GrailsQuery.SizeLessThan>()
        public static
        final SizeCriterionHandler<GrailsQuery.SizeLessThanEquals> LESS_THAN_EQUALS = new SizeCriterionHandler<GrailsQuery.SizeLessThanEquals>()

        SizeCriterionHandler() {
        }

        @Override
        Clause handle(OrientDbPersistentEntity entity, T criterion, Query query) {
            Association association = entity.getPropertyByName(criterion.property) as Association
            null
        }
    }
} 
