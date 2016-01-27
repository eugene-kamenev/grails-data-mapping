package org.grails.datastore.gorm.orient.engine

import com.github.raymanrt.orientqb.query.*
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.orient.OrientPersistentEntity
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.query.AssociationQuery
import org.grails.datastore.mapping.query.Query as GrailsQuery

import static com.github.raymanrt.orientqb.query.Clause.*
import static com.github.raymanrt.orientqb.query.Projection.projection
import static com.github.raymanrt.orientqb.query.ProjectionFunction.*

/**
 * OrientDB SQL Query builder based on @raymanrt orientqb project
 *
 * @author eugenekamenev
 */
@CompileStatic
class OrientQueryBuilder extends Query {
    protected final OrientPersistentEntity entity
    protected final Map<String, ?> namedParameters = [:]

    OrientQueryBuilder(OrientPersistentEntity entity) {
        this.entity = entity
        from(entity.className)
    }

    /**
     * Main method that populates query object
     *
     * @param projectionList
     * @param criterion
     * @param queryArgs
     * @return
     */
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
                for (value in (Map) queryArgs.sort) {
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

    /**
     * Method applies projections on query
     *
     * @param projections
     * @param queryArgs
     * @return
     */
    Query applyProjections(GrailsQuery.ProjectionList projections, Map queryArgs) {
        for (projection in projections.projectionList) {
            def handler = PROJECT_HANDLERS.get(projection.class)
            if (handler != null) {
                handler.handle(entity, projection, this)
            } else {
                throw new UnsupportedOperationException("Criterion of type ${projection.class.name} are not supported by GORM for OrientDb")
            }
        }
        this
    }

    /**
     * Method applies criterions on query
     *
     * @param junction
     * @param queryArgs
     * @return
     */
    Query applyCriterions(GrailsQuery.Junction junction, Map queryArgs) {
        for (criterion in junction.criteria) {
            def handler = CRITERION_HANDLERS.get(criterion.class)
            if (handler != null) {
                def clause = handler.handle(entity, criterion, this)
                if (clause) {
                    this.where(clause)
                    continue;
                }
            }
            throw new UnsupportedOperationException("Criterion of type ${criterion.class.name} with values: ${criterion} is not supported by GORM for OrientDb")
        }
        this
    }

    protected
    static Map<Class<? extends GrailsQuery.Projection>, ProjectionHandler> PROJECT_HANDLERS = [
            (GrailsQuery.CountProjection)        : new ProjectionHandler<GrailsQuery.CountProjection>() {
                @Override
                @CompileStatic
                def handle(OrientPersistentEntity entity, GrailsQuery.CountProjection countProjection, OrientQueryBuilder query) {
                    query.select(count(Projection.ALL))
                }
            },
            (GrailsQuery.CountDistinctProjection): new ProjectionHandler<GrailsQuery.CountDistinctProjection>() {
                @Override
                @CompileStatic
                def handle(OrientPersistentEntity entity, GrailsQuery.CountDistinctProjection countDistinctProjection, OrientQueryBuilder query) {
                    query.select(count(distinct(projection(entity.getNativePropertyName(countDistinctProjection.propertyName)))))
                }
            },
            (GrailsQuery.MinProjection)          : new ProjectionHandler<GrailsQuery.MinProjection>() {
                @Override
                @CompileStatic
                def handle(OrientPersistentEntity entity, GrailsQuery.MinProjection minProjection, OrientQueryBuilder query) {
                    query.select(min(projection(entity.getNativePropertyName(minProjection.propertyName))))
                }
            },
            (GrailsQuery.MaxProjection)          : new ProjectionHandler<GrailsQuery.MaxProjection>() {
                @Override
                @CompileStatic
                def handle(OrientPersistentEntity entity, GrailsQuery.MaxProjection maxProjection, OrientQueryBuilder query) {
                    query.select(max(projection(entity.getNativePropertyName(maxProjection.propertyName))))
                }
            },
            (GrailsQuery.SumProjection)          : new ProjectionHandler<GrailsQuery.SumProjection>() {
                @Override
                @CompileStatic
                def handle(OrientPersistentEntity entity, GrailsQuery.SumProjection sumProjection, OrientQueryBuilder query) {
                    query.select(sum(projection(entity.getNativePropertyName(sumProjection.propertyName))))
                }
            },
            (GrailsQuery.AvgProjection)          : new ProjectionHandler<GrailsQuery.AvgProjection>() {
                @Override
                @CompileStatic
                def handle(OrientPersistentEntity entity, GrailsQuery.AvgProjection avgProjection, OrientQueryBuilder query) {
                    query.select(avg(projection(entity.getNativePropertyName(avgProjection.propertyName))))
                }
            },
            (GrailsQuery.PropertyProjection)     : new ProjectionHandler<GrailsQuery.PropertyProjection>() {
                @Override
                @CompileStatic
                def handle(OrientPersistentEntity entity, GrailsQuery.PropertyProjection propertyProjection, OrientQueryBuilder query) {
                    def propertyName = propertyProjection.propertyName
                    def association = entity.getPropertyByName(propertyName)
                    if (association instanceof Association) {
                        query.select(expand(projection(entity.getNativePropertyName(propertyName))))
                    } else {
                        query.select(projection(entity.getNativePropertyName(propertyName)))
                    }
                }
            },
            (GrailsQuery.IdProjection)           : new ProjectionHandler<GrailsQuery.IdProjection>() {
                @Override
                def handle(OrientPersistentEntity entity, GrailsQuery.IdProjection IdProjection, OrientQueryBuilder query) {
                    query.select(Variable.rid())
                }
            }
    ]

    public
    static Map<Class<? extends GrailsQuery.Criterion>, CriterionHandler> CRITERION_HANDLERS = [
            (GrailsQuery.Conjunction)              : new CriterionHandler<GrailsQuery.Conjunction>() {
                @Override
                @CompileStatic
                Clause handle(OrientPersistentEntity entity, GrailsQuery.Conjunction criterion, OrientQueryBuilder query) {
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
                Clause handle(OrientPersistentEntity entity, GrailsQuery.Disjunction criterion, OrientQueryBuilder query) {
                    def inner = ((GrailsQuery.Junction) criterion).criteria.collect {GrailsQuery.Criterion it ->
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
                Clause handle(OrientPersistentEntity entity, GrailsQuery.Negation criterion, OrientQueryBuilder query) {
                    List<GrailsQuery.Criterion> criteria = criterion.criteria
                    def disjunction = new GrailsQuery.Disjunction(criteria)
                    CriterionHandler<GrailsQuery.Disjunction> handler = {->
                        CRITERION_HANDLERS.get(GrailsQuery.Disjunction)
                    }.call()
                    not(handler.handle(entity, disjunction, query))
                }
            },
            (GrailsQuery.Equals)                   : new CriterionHandler<GrailsQuery.Equals>() {
                @Override
                @CompileStatic
                Clause handle(OrientPersistentEntity entity, GrailsQuery.Equals criterion, OrientQueryBuilder query) {
                    addTypedClause(entity, criterion.property, Operator.EQ, criterion.value, query)
                }
            },
            (GrailsQuery.IdEquals)                 : new CriterionHandler<GrailsQuery.IdEquals>() {
                @Override
                @CompileStatic
                Clause handle(OrientPersistentEntity entity, GrailsQuery.IdEquals criterion, OrientQueryBuilder query) {
                    addTypedClause(entity, criterion.property, Operator.EQ, criterion.value, query)
                }
            },
            (GrailsQuery.Like)                     : new CriterionHandler<GrailsQuery.Like>() {
                @Override
                @CompileStatic
                Clause handle(OrientPersistentEntity entity, GrailsQuery.Like criterion, OrientQueryBuilder query) {
                    addTypedClause(entity, criterion.property, Operator.LIKE, criterion.value, query)
                }
            },
            (GrailsQuery.ILike)                    : new CriterionHandler<GrailsQuery.ILike>() {
                @Override
                @CompileStatic
                Clause handle(OrientPersistentEntity entity, GrailsQuery.ILike criterion, OrientQueryBuilder query) {
                    addTypedClause(entity, criterion.property, Operator.LIKE, criterion.value, query)
                }
            },
            (GrailsQuery.RLike)                    : new CriterionHandler<GrailsQuery.RLike>() {
                @Override
                @CompileStatic
                Clause handle(OrientPersistentEntity entity, GrailsQuery.RLike criterion, OrientQueryBuilder query) {
                    addTypedClause(entity, criterion.property, Operator.LIKE, criterion.value, query)
                }
            },
            (GrailsQuery.In)                       : new CriterionHandler<GrailsQuery.In>() {
                @Override
                @CompileStatic
                Clause handle(OrientPersistentEntity entity, GrailsQuery.In criterion, OrientQueryBuilder query) {
                    addTypedClause(entity, criterion.property, Operator.IN, criterion.values, query)
                }
            },
            (GrailsQuery.IsNull)                   : new CriterionHandler<GrailsQuery.IsNull>() {
                @Override
                @CompileStatic
                Clause handle(OrientPersistentEntity entity, GrailsQuery.IsNull criterion, OrientQueryBuilder query) {
                    projection(entity.getNativePropertyName(criterion.property)).isNull()
                }
            },
            (AssociationQuery)                     : new AssociationQueryHandler(),
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
                Clause handle(OrientPersistentEntity entity, GrailsQuery.Between criterion, OrientQueryBuilder query) {
                    if (criterion.from instanceof Number && criterion.to instanceof Number) {
                        return projection(entity.getNativePropertyName(criterion.property)).between((Number) criterion.from, (Number) criterion.to)
                    }
                    if (criterion.from instanceof Date && criterion.to instanceof Date) {
                        return and(ComparisonCriterionHandler.LESS_THAN_EQUALS.handle(entity, new GrailsQuery.LessThanEquals(criterion.property, criterion.to), query),
                                ComparisonCriterionHandler.GREATER_THAN_EQUALS.handle(entity, new GrailsQuery.GreaterThanEquals(criterion.property, criterion.from), query))
                    }
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
     * @param < T >    The projection type
     */
    static interface ProjectionHandler<T extends GrailsQuery.Projection> {
        def handle(OrientPersistentEntity entity, T projection, OrientQueryBuilder query)
    }

    /**
     * Interface for handling criterion when building OrientDb queries
     *
     * @param < T >    The criterion type
     */
    static interface CriterionHandler<T extends GrailsQuery.Criterion> {
        Clause handle(OrientPersistentEntity entity, T criterion, OrientQueryBuilder query)
    }

    /**
     * Handles AssociationQuery instances
     */
    @CompileStatic
    static class AssociationQueryHandler implements CriterionHandler<AssociationQuery> {
        @Override
        Clause handle(OrientPersistentEntity entity, AssociationQuery criterion, OrientQueryBuilder query) {
            AssociationQuery aq = criterion as AssociationQuery
            return CRITERION_HANDLERS.get(aq.criteria.getClass()).handle(entity, aq.criteria, query)
        }
    }

    /**
     * A criterion handler for comparison criterion
     *
     * @param < T >
     */
    @CompileStatic
    static
    abstract class ComparisonCriterionHandler<T extends GrailsQuery.PropertyCriterion> implements CriterionHandler<T> {
        public static
        final ComparisonCriterionHandler<GrailsQuery.GreaterThanEquals> GREATER_THAN_EQUALS = new ComparisonCriterionHandler<GrailsQuery.GreaterThanEquals>() {
            @Override
            Clause handle(OrientPersistentEntity entity, GrailsQuery.GreaterThanEquals criterion, OrientQueryBuilder query) {
                addTypedClause(entity, criterion.property, Operator.GE, criterion.value, query)
            }
        }
        public static
        final ComparisonCriterionHandler<GrailsQuery.GreaterThan> GREATER_THAN = new ComparisonCriterionHandler<GrailsQuery.GreaterThan>() {
            @Override
            Clause handle(OrientPersistentEntity entity, GrailsQuery.GreaterThan criterion, OrientQueryBuilder query) {
                addTypedClause(entity, criterion.property, Operator.GT, criterion.value, query)
            }
        }
        public static
        final ComparisonCriterionHandler<GrailsQuery.LessThan> LESS_THAN = new ComparisonCriterionHandler<GrailsQuery.LessThan>() {
            @Override
            Clause handle(OrientPersistentEntity entity, GrailsQuery.LessThan criterion, OrientQueryBuilder query) {
                addTypedClause(entity, criterion.property, Operator.LT, criterion.value, query)
            }
        }
        public static
        final ComparisonCriterionHandler<GrailsQuery.LessThanEquals> LESS_THAN_EQUALS = new ComparisonCriterionHandler<GrailsQuery.LessThanEquals>() {
            @Override
            Clause handle(OrientPersistentEntity entity, GrailsQuery.LessThanEquals criterion, OrientQueryBuilder query) {
                addTypedClause(entity, criterion.property, Operator.LE, criterion.value, query)
            }
        }
        public static
        final ComparisonCriterionHandler<GrailsQuery.NotEquals> NOT_EQUALS = new ComparisonCriterionHandler<GrailsQuery.NotEquals>() {
            @Override
            Clause handle(OrientPersistentEntity entity, GrailsQuery.NotEquals criterion, OrientQueryBuilder query) {
                addTypedClause(entity, criterion.property, Operator.NE, criterion.value, query)
            }
        }
        public static
        final ComparisonCriterionHandler<GrailsQuery.Equals> EQUALS = new ComparisonCriterionHandler<GrailsQuery.Equals>() {
            @Override
            Clause handle(OrientPersistentEntity entity, GrailsQuery.Equals criterion, OrientQueryBuilder query) {
                addTypedClause(entity, criterion.property, Operator.EQ, criterion.value, query)
            }
        }

        abstract Clause handle(OrientPersistentEntity entity, T criterion, OrientQueryBuilder query)
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
        Clause handle(OrientPersistentEntity entity, T criterion, OrientQueryBuilder query) {
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

        Operator operator

        public static
        final SizeCriterionHandler<GrailsQuery.SizeEquals> EQUALS = new SizeCriterionHandler<GrailsQuery.SizeEquals>(Operator.EQ);
        public static
        final SizeCriterionHandler<GrailsQuery.SizeNotEquals> NOT_EQUALS = new SizeCriterionHandler<GrailsQuery.SizeNotEquals>(Operator.NE)
        public static
        final SizeCriterionHandler<GrailsQuery.SizeGreaterThan> GREATER_THAN = new SizeCriterionHandler<GrailsQuery.SizeGreaterThan>(Operator.GT)
        public static
        final SizeCriterionHandler<GrailsQuery.SizeGreaterThanEquals> GREATER_THAN_EQUALS = new SizeCriterionHandler<GrailsQuery.SizeGreaterThanEquals>(Operator.GE)
        public static
        final SizeCriterionHandler<GrailsQuery.SizeLessThan> LESS_THAN = new SizeCriterionHandler<GrailsQuery.SizeLessThan>(Operator.LT)
        public static
        final SizeCriterionHandler<GrailsQuery.SizeLessThanEquals> LESS_THAN_EQUALS = new SizeCriterionHandler<GrailsQuery.SizeLessThanEquals>(Operator.LE)

        SizeCriterionHandler(Operator operator) {
            this.operator = operator
        }

        @Override
        Clause handle(OrientPersistentEntity entity, T criterion, OrientQueryBuilder query) {
            Association association = entity.getPropertyByName(criterion.property) as Association
            def nativeName = entity.getNativePropertyName(criterion.property)
            def paramKey = query.addToParams(criterion.property, criterion.value)
            return clause(projection(entity.getNativePropertyName(criterion.property)).size(), this.operator, Parameter.parameter(paramKey))
        }
    }

    /**
     * Add named parameter to query, parameters should be named different
     *
     * @param name
     * @param value
     * @param index
     * @return
     */
    public String addToParams(String name, Object value, int index = 0) {
        def key = "${name}_${index}".toString()
        if (!namedParameters[key]) {
            namedParameters[key] = value
            return key
        } else {
            addToParams(name, value, index + 1)
        }
    }

    public
    static Clause addTypedClause(OrientPersistentEntity entity, String property, Operator operator, Object value, OrientQueryBuilder query) {
        def paramKey = query.addToParams(property, value)
        clause(projection(entity.getNativePropertyName(property)), operator, Parameter.parameter(paramKey))
    }

} 
