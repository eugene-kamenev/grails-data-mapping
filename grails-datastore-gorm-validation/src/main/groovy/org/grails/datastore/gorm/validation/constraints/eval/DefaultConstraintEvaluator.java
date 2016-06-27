package org.grails.datastore.gorm.validation.constraints.eval;

import grails.gorm.validation.Constrained;
import grails.gorm.validation.ConstrainedProperty;
import grails.gorm.validation.exceptions.ValidationConfigurationException;
import groovy.lang.*;
import grails.gorm.validation.DefaultConstrainedProperty;
import org.grails.datastore.gorm.validation.constraints.builder.ConstrainedPropertyBuilder;
import org.grails.datastore.gorm.validation.constraints.registry.ConstraintRegistry;
import org.grails.datastore.mapping.config.Property;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.config.GormProperties;
import org.grails.datastore.mapping.model.types.Identity;
import org.grails.datastore.mapping.model.types.ToOne;
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Evaluates constraints for entities
 *
 * @author Graeme Rocher
 * @since 6.0
 */
public class DefaultConstraintEvaluator implements ConstraintsEvaluator {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultConstraintEvaluator.class);

    protected final ConstraintRegistry constraintRegistry;
    protected final MappingContext mappingContext;
    protected final Map<String, Object> defaultConstraints;

    public DefaultConstraintEvaluator(ConstraintRegistry constraintRegistry, MappingContext mappingContext, Map<String, Object> defaultConstraints) {
        this.constraintRegistry = constraintRegistry;
        this.mappingContext = mappingContext;
        this.defaultConstraints = defaultConstraints;
    }

    @Override
    public Map<String, Object> getDefaultConstraints() {
        return null;
    }

    @Override
    public Map<String, ConstrainedProperty> evaluate(@SuppressWarnings("rawtypes") Class cls) {
        return evaluate(cls, false);
    }

    @Override
    public Map<String, ConstrainedProperty> evaluate(@SuppressWarnings("rawtypes") Class theClass, boolean defaultNullable) {
        LinkedList<Class<?>> classChain = getSuperClassChain(theClass);
        Class<?> clazz;

        ConstrainedPropertyBuilder delegate = new ConstrainedPropertyBuilder(this.mappingContext, this.constraintRegistry, theClass, defaultConstraints);

        // Evaluate all the constraints closures in the inheritance chain
        for (Class aClassChain : classChain) {
            clazz = (Class<?>) aClassChain;
            Closure<?> c = (Closure<?>) ClassPropertyFetcher.forClass(aClassChain).getStaticPropertyValue(PROPERTY_NAME, Closure.class);

            if (c != null) {
                c = (Closure<?>) c.clone();
                c.setResolveStrategy(Closure.DELEGATE_ONLY);
                c.setDelegate(delegate);
                c.call();
            }
            else {
                LOG.debug("User-defined constraints not found on class [" + clazz + "], applying default constraints");
            }
        }

        Map<String, ConstrainedProperty> constrainedProperties = delegate.getConstrainedProperties();
        PersistentEntity entity = mappingContext.getPersistentEntity(theClass.getName());
        List<PersistentProperty> properties = null;
        if(entity != null) {

            properties = entity.getPersistentProperties();
            if (properties != null && !(constrainedProperties.isEmpty())) {

                for (PersistentProperty p : properties) {
                    // assume no formula issues if Hibernate isn't available to avoid CNFE
                    Property mappedForm = p.getMapping().getMappedForm();
                    if (canPropertyBeConstrained(p)) {
                        if (mappedForm.isDerived()) {
                            if (constrainedProperties.remove(p.getName()) != null) {
                                LOG.warn("Derived properties may not be constrained. Property [" + p.getName() + "] of domain class " + theClass.getName() + " will not be checked during validation.");
                            }
                        } else {
                            final String propertyName = p.getName();
                            ConstrainedProperty cp = constrainedProperties.get(propertyName);
                            if (cp == null) {
                                DefaultConstrainedProperty constrainedProperty = new DefaultConstrainedProperty(entity.getJavaClass(), propertyName, p.getType(), constraintRegistry);
                                cp = constrainedProperty;
                                constrainedProperty.setOrder(constrainedProperties.size() + 1);
                                constrainedProperties.put(propertyName, cp);
                            }
                            // Make sure all fields are required by default, unless
                            // specified otherwise by the constraints
                            // If the field is a Java entity annotated with @Entity skip this
                            applyDefaultConstraints(propertyName, p, cp, defaultConstraints);
                        }
                    }
                }
            }
        }

        if (properties == null || properties.size() == 0) {
            final Set<Map.Entry<String, ConstrainedProperty>> entrySet = constrainedProperties.entrySet();
            for (Map.Entry<String, ConstrainedProperty> entry : entrySet) {
                final ConstrainedProperty constrainedProperty = entry.getValue();
                if (!constrainedProperty.hasAppliedConstraint(ConstrainedProperty.NULLABLE_CONSTRAINT)) {
                    applyDefaultNullableConstraint(constrainedProperty, defaultNullable);
                }
            }
        }

        applySharedConstraints(delegate, constrainedProperties);

        return constrainedProperties;
    }

    protected void applySharedConstraints(
            ConstrainedPropertyBuilder constrainedPropertyBuilder,
            Map<String, ConstrainedProperty> constrainedProperties) {
        for (Map.Entry<String, ConstrainedProperty> entry : constrainedProperties.entrySet()) {
            String propertyName = entry.getKey();
            Constrained constrainedProperty = entry.getValue();
            String sharedConstraintReference = constrainedPropertyBuilder.getSharedConstraint(propertyName);
            if (sharedConstraintReference != null && defaultConstraints !=  null) {
                Object o = defaultConstraints.get(sharedConstraintReference);
                if (o instanceof Map) {
                    @SuppressWarnings({ "unchecked", "rawtypes" })
                    Map<String, Object> constraintsWithinSharedConstraint = (Map) o;
                    for (Map.Entry<String, Object> e : constraintsWithinSharedConstraint.entrySet()) {
                        constrainedProperty.applyConstraint(e.getKey(), e.getValue());
                    }
                } else {
                    throw new ValidationConfigurationException("Property [" +
                            constrainedProperty.getOwner().getName() + '.' + propertyName +
                            "] references shared constraint [" + sharedConstraintReference +
                            ":" + o + "], which doesn't exist!");
                }
            }
        }
    }

    protected boolean canPropertyBeConstrained(PersistentProperty property) {
        return true;
    }

    public static LinkedList<Class<?>> getSuperClassChain(Class<?> theClass) {
        LinkedList<Class<?>> classChain = new LinkedList<>();
        Class<?> clazz = theClass;
        while (clazz != Object.class && clazz != null) {
            classChain.addFirst(clazz);
            clazz = clazz.getSuperclass();
        }
        return classChain;
    }



    @SuppressWarnings("unchecked")
    protected void applyDefaultConstraints(String propertyName, PersistentProperty p,
                                           ConstrainedProperty cp, Map<String, Object> defaultConstraints) {

        if (defaultConstraints != null && !defaultConstraints.isEmpty()) {
            if (defaultConstraints.containsKey("*")) {
                final Object o = defaultConstraints.get("*");
                if (o instanceof Map) {
                    Map<String, Object> globalConstraints = (Map<String, Object>)o;
                    applyMapOfConstraints(globalConstraints, propertyName, p, cp);
                }
            }
        }

        if (canApplyNullableConstraint(propertyName, p, cp)) {
            applyDefaultNullableConstraint(p, cp);
        }
    }

    protected void applyDefaultNullableConstraint(PersistentProperty p, ConstrainedProperty cp) {
        applyDefaultNullableConstraint(cp, false);
    }

    protected void applyDefaultNullableConstraint(ConstrainedProperty cp, boolean defaultNullable) {
        boolean isCollection = Collection.class.isAssignableFrom(cp.getPropertyType()) || Map.class.isAssignableFrom(cp.getPropertyType());
        cp.applyConstraint(ConstrainedProperty.NULLABLE_CONSTRAINT, isCollection || defaultNullable);
    }

    protected boolean canApplyNullableConstraint(String propertyName, PersistentProperty property, Constrained constrained) {
        if (property == null || property.getType() == null) return false;

        final PersistentEntity domainClass = property.getOwner();
        // only apply default nullable to Groovy entities not legacy Java ones
        if (!GroovyObject.class.isAssignableFrom(domainClass.getJavaClass())) return false;

        final PersistentProperty versionProperty = domainClass.getVersion();
        final boolean isVersion = versionProperty != null && versionProperty.equals(property);
        return !constrained.hasAppliedConstraint(ConstrainedProperty.NULLABLE_CONSTRAINT) &&
                isConstrainableProperty(property, propertyName) && !isVersion;
    }

    protected void applyMapOfConstraints(Map<String, Object> constraints, String propertyName, PersistentProperty p, ConstrainedProperty cp) {
        for (Map.Entry<String, Object> entry : constraints.entrySet()) {
            String constraintName = entry.getKey();
            Object constrainingValue = entry.getValue();
            if (!cp.hasAppliedConstraint(constraintName) && cp.supportsContraint(constraintName)) {
                if (ConstrainedProperty.NULLABLE_CONSTRAINT.equals(constraintName)) {
                    if (isConstrainableProperty(p,propertyName)) {
                        cp.applyConstraint(constraintName, constrainingValue);
                    }
                }
                else {
                    cp.applyConstraint(constraintName,constrainingValue);
                }
            }
        }
    }

    protected boolean isConstrainableProperty(PersistentProperty p, String propertyName) {

        return !propertyName.equals(GormProperties.VERSION) &&
                !propertyName.equals(GormProperties.DATE_CREATED) &&
                !propertyName.equals(GormProperties.LAST_UPDATED) &&
                !(p instanceof Identity) &&
                !(p.getMapping().getMappedForm().isDerived()) &&
                !( (p instanceof ToOne) && ((ToOne)p).isBidirectional());
    }

}
