/* Copyright (C) 2011 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grails.datastore.gorm.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import grails.core.GrailsDomainClass;
import grails.core.GrailsDomainClassProperty;
import org.grails.datastore.gorm.validation.ValidatorProvider;
import org.grails.datastore.mapping.config.Entity;
import org.grails.datastore.mapping.config.Property;
import org.grails.datastore.mapping.model.*;
import org.grails.datastore.mapping.model.types.*;
import org.grails.datastore.mapping.reflect.EntityReflector;
import org.springframework.validation.Validator;

import javax.persistence.FetchType;

/**
 * Bridges the {@link GrailsDomainClass} interface into the {@link PersistentEntity} interface
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings("rawtypes")
public class GrailsDomainClassPersistentEntity implements PersistentEntity, ValidatorProvider {

    private GrailsDomainClass domainClass;
    private GrailsDomainClassMappingContext mappingContext;
    private GrailsDomainClassPersistentProperty identifier;
    private GrailsDomainClassPersistentProperty version;
    private Map<String, PersistentProperty> propertiesByName = new HashMap<String, PersistentProperty>();
    private List<PersistentProperty> properties = new ArrayList<PersistentProperty>();
    private List<Association> associations = new ArrayList<Association>();
    private boolean initialized;
    private final ClassMapping<Entity> classMapping;

    public GrailsDomainClassPersistentEntity(final GrailsDomainClass domainClass,
                                             GrailsDomainClassMappingContext mappingContext) {
        this.domainClass = domainClass;
        this.mappingContext = mappingContext;
        this.classMapping = new ClassMapping<Entity>() {
            @Override
            public PersistentEntity getEntity() {
                return GrailsDomainClassPersistentEntity.this;
            }

            @Override
            public Entity getMappedForm() {
                return new Entity();
            }

            @Override
            public IdentityMapping getIdentifier() {
                return new IdentityMapping() {
                    @Override
                    public String[] getIdentifierName() {
                        return new String[]{identifier.getName()};
                    }

                    @Override
                    public ValueGenerator getGenerator() {
                        return ValueGenerator.AUTO;
                    }

                    @Override
                    public ClassMapping getClassMapping() {
                        return classMapping;
                    }

                    @Override
                    public Property getMappedForm() {
                        return identifier.getMapping().getMappedForm();
                    }
                };
            }
        };
    }

    public String getMappingStrategy() {
        return domainClass.getMappingStrategy();
    }

    public boolean isAbstract() {
        return domainClass.isAbstract();
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PersistentEntity && getName().equals(((PersistentEntity) obj).getName());
    }

    @Override
    public Validator getValidator() {
        return getDomainClass().getValidator();
    }

    /**
     * @return The wrapped GrailsDomainClass instance
     */
    public GrailsDomainClass getDomainClass() {
        return domainClass;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void initialize() {
        if (domainClass.getIdentifier() != null) {
            identifier = new GrailsDomainClassPersistentProperty(this, domainClass.getIdentifier());
            propertiesByName.put(identifier.getName(), identifier);
        }
        if (domainClass.getVersion() != null) {
            version = new GrailsDomainClassPersistentProperty(this, domainClass.getVersion());
            propertiesByName.put(version.getName(), version);
        }

        mappingContext.addEntityValidator(this, domainClass.getValidator());

        final GrailsDomainClassProperty[] persistentProperties = domainClass.getPersistentProperties();
        for (GrailsDomainClassProperty grailsDomainClassProperty : persistentProperties) {
            PersistentProperty persistentProperty;
            if (grailsDomainClassProperty.isAssociation()) {
                if (grailsDomainClassProperty.isEmbedded()) {
                    persistentProperty = createEmbedded(mappingContext, grailsDomainClassProperty);
                } else if (grailsDomainClassProperty.isOneToMany()) {
                    persistentProperty = createOneToMany(mappingContext, grailsDomainClassProperty);
                } else if (grailsDomainClassProperty.isHasOne()) {
                    persistentProperty = createOneToOne(mappingContext, grailsDomainClassProperty);
                } else if (grailsDomainClassProperty.isOneToOne()) {
                    persistentProperty = createOneToOne(mappingContext, grailsDomainClassProperty);
                } else if (grailsDomainClassProperty.isManyToOne()) {
                    persistentProperty = createManyToOne(mappingContext, grailsDomainClassProperty);
                } else if (grailsDomainClassProperty.isManyToMany()) {
                    persistentProperty = createManyToMany(mappingContext, grailsDomainClassProperty);
                } else {
                    persistentProperty = new GrailsDomainClassPersistentProperty(this, grailsDomainClassProperty);
                }
            }
            else if(grailsDomainClassProperty.isBasicCollectionType()) {
                persistentProperty = createBasicCollection(mappingContext, grailsDomainClassProperty);
            }
            else {
                persistentProperty = new GrailsDomainClassPersistentProperty(this, grailsDomainClassProperty);
            }
            propertiesByName.put(grailsDomainClassProperty.getName(), persistentProperty);
            properties.add(persistentProperty);

            if (persistentProperty instanceof Association) {
                associations.add((Association) persistentProperty);
            }
        }
        initialized = true;
    }


    public PersistentProperty[] getCompositeIdentity() {
        return null;
    }

    public String getName() {
        return domainClass.getFullName();
    }

    public PersistentProperty getIdentity() {
        return identifier;
    }

    public PersistentProperty getVersion() {
        return version;
    }

    public boolean isVersioned() {
        // TODO
        return version != null;
    }

    public List<PersistentProperty> getPersistentProperties() {
        return properties;
    }

    public List<Association> getAssociations() {
        return associations;
    }

    public PersistentProperty getPropertyByName(String name) {
        return propertiesByName.get(name);
    }

    public Class getJavaClass() {
        return domainClass.getClazz();
    }

    public boolean isInstance(Object obj) {
        return domainClass.getClazz().isInstance(obj);
    }

    public ClassMapping getMapping() {
        return classMapping;
    }

    public Object newInstance() {
        return domainClass.newInstance();
    }

    public List<String> getPersistentPropertyNames() {
        return new ArrayList<String>(propertiesByName.keySet());
    }

    public String getDecapitalizedName() {
        return domainClass.getLogicalPropertyName();
    }

    public boolean isOwningEntity(PersistentEntity owner) {
        return domainClass.isOwningClass(owner.getJavaClass());
    }

    public PersistentEntity getParentEntity() {
        if (!isRoot()) {
            return getMappingContext().getPersistentEntity(
                    getJavaClass().getSuperclass().getName());
        }
        return null;
    }

    public PersistentEntity getRootEntity() {
        if (isRoot() || getParentEntity() == null) {
            return this;
        }
        PersistentEntity parent = getParentEntity();
        while (!parent.isRoot() && parent.getParentEntity() != null) {
            parent = parent.getParentEntity();
        }
        return parent;
    }

    public boolean isRoot() {
        return domainClass.isRoot();
    }

    public String getDiscriminator() {
        return getName();
    }

    public MappingContext getMappingContext() {
        return mappingContext;
    }

    public boolean hasProperty(String name, Class type) {
        return domainClass.hasProperty(name);
    }

    public boolean isIdentityName(String propertyName) {
        return domainClass.getIdentifier().getName().equals(propertyName);
    }

    private PersistentProperty createManyToOne(
            GrailsDomainClassMappingContext ctx,
            GrailsDomainClassProperty grailsDomainClassProperty) {
        final PropertyMapping<Property> mapping = createDefaultMapping(grailsDomainClassProperty);
        final ManyToOne oneToOne = new ManyToOne(this, ctx, grailsDomainClassProperty.getName(), grailsDomainClassProperty.getType()) {
            public PropertyMapping getMapping() {
                return mapping;
            }
        };
        configureAssociation(grailsDomainClassProperty, oneToOne);
        return oneToOne;
    }

    private PersistentProperty createBasicCollection(GrailsDomainClassMappingContext mappingContext, GrailsDomainClassProperty grailsDomainClassProperty) {
        final PropertyMapping<Property> mapping = createDefaultMapping(grailsDomainClassProperty);
        final Basic basic = new Basic(this, mappingContext, grailsDomainClassProperty.getName(), grailsDomainClassProperty.getType()) {
            public PropertyMapping getMapping() {
                return mapping;
            }
        };
        return basic;
    }


    private PersistentProperty createManyToMany(
            GrailsDomainClassMappingContext ctx,
            GrailsDomainClassProperty grailsDomainClassProperty) {
        final PropertyMapping<Property> mapping = createDefaultMapping(grailsDomainClassProperty);
        final ManyToMany manyToMany = new ManyToMany(this, ctx, grailsDomainClassProperty.getName(), grailsDomainClassProperty.getType()) {
            public PropertyMapping getMapping() {
                return mapping;
            }
        };
        configureAssociation(grailsDomainClassProperty, manyToMany);
        return manyToMany;
    }

    private PersistentProperty createOneToOne(
            GrailsDomainClassMappingContext ctx,
            GrailsDomainClassProperty grailsDomainClassProperty) {
        final PropertyMapping<Property> mapping = createDefaultMapping(grailsDomainClassProperty);
        final OneToOne oneToOne = new OneToOne(this, ctx, grailsDomainClassProperty.getName(), grailsDomainClassProperty.getType()) {
            public PropertyMapping getMapping() {
                return mapping;
            }
        };
        configureAssociation(grailsDomainClassProperty, oneToOne);
        return oneToOne;
    }

    private OneToMany createOneToMany(GrailsDomainClassMappingContext mappingContext,
                                      GrailsDomainClassProperty grailsDomainClassProperty) {
        final PropertyMapping<Property> mapping = createDefaultMapping(grailsDomainClassProperty);
        final OneToMany oneToMany = new OneToMany(this, mappingContext, grailsDomainClassProperty.getName(), grailsDomainClassProperty.getType()) {

            public PropertyMapping getMapping() {
                return mapping;
            }
        };
        configureAssociation(grailsDomainClassProperty, oneToMany);

        return oneToMany;
    }

    private PropertyMapping<Property> createDefaultMapping(GrailsDomainClassProperty grailsDomainClassProperty) {
        final Property property = new Property();
        if(grailsDomainClassProperty.getFetchMode() == GrailsDomainClassProperty.FETCH_EAGER) {
            property.setFetchStrategy(FetchType.EAGER);
        }
        return new PropertyMapping<Property>() {
            @Override
            public ClassMapping getClassMapping() {
                return GrailsDomainClassPersistentEntity.this.getMapping();
            }

            @Override
            public Property getMappedForm() {
                return property;
            }
        };
    }

    private void configureAssociation(
            GrailsDomainClassProperty grailsDomainClassProperty,
            final Association association) {
        association.setAssociatedEntity(getMappingContext().addPersistentEntity(grailsDomainClassProperty.getReferencedPropertyType()));
        association.setOwningSide(grailsDomainClassProperty.isOwningSide());
        String referencedPropertyName = grailsDomainClassProperty.getReferencedPropertyName();
        if (referencedPropertyName != null) {
            association.setReferencedPropertyName(referencedPropertyName);
        } else {
            GrailsDomainClassProperty otherSide = grailsDomainClassProperty.getOtherSide();
            if (otherSide != null) {
                association.setReferencedPropertyName(otherSide.getName());
            }
        }
    }

    private PersistentProperty createEmbedded(
            GrailsDomainClassMappingContext mappingContext,
            GrailsDomainClassProperty grailsDomainClassProperty) {
        final PropertyMapping<Property> mapping = createDefaultMapping(grailsDomainClassProperty);
        Embedded persistentProperty = new Embedded(this, mappingContext, grailsDomainClassProperty.getName(), grailsDomainClassProperty.getType()) {
            public PropertyMapping getMapping() {
                return mapping;
            }
        };
        persistentProperty.setOwningSide(grailsDomainClassProperty.isOwningSide());
        persistentProperty.setReferencedPropertyName(grailsDomainClassProperty.getReferencedPropertyName());

        return persistentProperty;
    }

    public boolean isExternal() {
        return false;
    }

    public void setExternal(boolean external) {
        // do nothing
    }

    @Override
    public EntityReflector getReflector() {
        return getMappingContext().getEntityReflector(this);
    }
}
