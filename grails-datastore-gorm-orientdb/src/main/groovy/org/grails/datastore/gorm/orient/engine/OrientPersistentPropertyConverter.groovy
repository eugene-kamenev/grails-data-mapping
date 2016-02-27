package org.grails.datastore.gorm.orient.engine

import com.orientechnologies.orient.core.db.record.OIdentifiable
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument
import com.tinkerpop.blueprints.impls.orient.OrientEdge
import com.tinkerpop.blueprints.impls.orient.OrientElement
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.orient.OrientPersistentEntity
import org.grails.datastore.gorm.orient.OrientSession
import org.grails.datastore.gorm.orient.collection.OrientPersistentSet
import org.grails.datastore.gorm.orient.mapping.config.OrientAttribute
import org.grails.datastore.mapping.collection.PersistentList
import org.grails.datastore.mapping.collection.PersistentSet
import org.grails.datastore.mapping.collection.PersistentSortedSet
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.engine.AssociationQueryExecutor
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.engine.internal.MappingUtils
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.*

import javax.persistence.CascadeType
import javax.persistence.FetchType
/**
 * Main class that represents different marshall/unmarshall methods for associations and basic properties
 *
 * @author eugenekamenev
 */
@CompileStatic
abstract class OrientPersistentPropertyConverter {

    static final List<OType> linkedTypes = [OType.LINK, OType.LINKSET, OType.LINKLIST]

    private static final Map<Class, ?> BASIC_PROPERTY_CONVERTERS = [
            (Identity)          : new IdentityConverter(),
            (Simple)            : new SimpleConverter(),
            (Custom)            : new CustomTypeConverter(),
            (OneToMany)         : new OneToManyConverter(),
            (ManyToOne)         : new ToOneConverter(),
            (ManyToMany)        : null,
            (OneToOne)          : new ToOneConverter(),
            (Embedded)          : new EmbeddedConverter(),
            (EmbeddedCollection): new EmbeddedCollectionConverter(),
    ]

    private static final Map<Class, ?> LINK_ASSOCIATIONS_CONVERTERS = [
            (OneToMany) : new LinkedOneToManyConverter(),
            (ManyToMany): new LinkedManyToManyConverter(),
            (ManyToOne) : new LinkedManyToOneConverter(),
            (OneToOne)  : new LinkedOneToOneConverter()
    ]

    private static final Map<Class, ?> EDGE_ASSOCIATIONS_CONVERTERS = [
            (OneToMany) : new EdgeOneToManyConverter(),
            (ManyToMany): new EdgeManyToManyConverter(),
            (ManyToOne) : new EdgeManyToOneConverter(),
            (OneToOne)  : new EdgeOneToOneConverter()
    ]

    /**
     * Get Property Converter by persistent property
     *
     * @param property
     * @return
     */
    static PropertyConverter getForPersistentProperty(PersistentProperty property) {
        if (property instanceof Identity) {
            return (PropertyConverter) BASIC_PROPERTY_CONVERTERS[Identity]
        }
        if (property instanceof Simple) {
            return (PropertyConverter) BASIC_PROPERTY_CONVERTERS[Simple]
        }
        if (property instanceof Custom) {
            return (PropertyConverter) BASIC_PROPERTY_CONVERTERS[Custom]
        }
        if (property instanceof Embedded) {
            return (PropertyConverter) BASIC_PROPERTY_CONVERTERS[Embedded]
        }
        if (property instanceof EmbeddedCollection) {
            return (PropertyConverter) BASIC_PROPERTY_CONVERTERS[EmbeddedCollection]
        }
        Class associationClass = null
        switch (property) {
            case OneToOne: associationClass = OneToOne; break;
            case OneToMany: associationClass = OneToMany; break;
            case ManyToOne: associationClass = ManyToOne; break;
            case ManyToMany: associationClass = ManyToMany; break;
        }
        if (property instanceof Association) {
            def mapping = (OrientAttribute) property.mapping.mappedForm
            if (mapping.edge != null) {
                return getForEdge(associationClass)
            }
            if (mapping.type in linkedTypes) {
                return getLinked(associationClass)
            }
            return getBasic(associationClass)
        }
        return null
    }

    /**
     * Get basic property converter
     *
     * @param associationClass
     * @return
     */
    static PropertyConverter getBasic(Class associationClass) {
        return (PropertyConverter) BASIC_PROPERTY_CONVERTERS[associationClass]
    }

    /**
     * Get linked association converter
     *
     * @param associationClass
     * @return
     */
    static PropertyConverter getLinked(Class associationClass) {
        return (PropertyConverter) LINK_ASSOCIATIONS_CONVERTERS[associationClass]
    }

    /**
     * Get edge handling converter
     *
     * @param associationClass
     * @return
     */
    static PropertyConverter getForEdge(Class associationClass) {
        return (PropertyConverter) EDGE_ASSOCIATIONS_CONVERTERS[associationClass]
    }

    /**
     * A {@link PropertyConverter} capable of decoding the {@link org.grails.datastore.mapping.model.types.Identity}
     */
    static class IdentityConverter implements PropertyConverter<Identity> {

        @Override
        void marshall(OIdentifiable nativeEntry, Identity property, EntityAccess entityAccess, OrientSession session) {
            // do nothing here, because nativeEntry will have this property already
        }

        @Override
        void unmarshall(OIdentifiable nativeEntry, Identity property, EntityAccess entityAccess, OrientSession session) {
            switch (property.type) {
                case Number:
                    throw new UnsupportedOperationException("OrientDB does not support numeric id fields, use ${ORecordId.class.name} or ${String.class.name}")
                case String: entityAccess.setIdentifierNoConversion(nativeEntry.identity.toString());
                    break;
                case ORecordId: entityAccess.setIdentifierNoConversion(nativeEntry.identity);
                    break;
            }
        }
    }

    /**
     * A {@PropertyDecoder} capable of decoding {@link org.grails.datastore.mapping.model.types.Simple} properties
     */
    static class SimpleConverter implements PropertyConverter<Simple> {

        public static final Map<Class, SimpleTypeConverter> SIMPLE_TYPE_CONVERTERS = [:]

        public static final SimpleTypeConverter DEFAULT_CONVERTER = new SimpleTypeConverter() {
            @Override
            void marshall(OIdentifiable oIdentifiable, Simple property, EntityAccess entityAccess) {
                setValue(oIdentifiable, property, entityAccess.getProperty(property.name))
            }

            @Override
            void unmarshall(OIdentifiable oIdentifiable, Simple property, EntityAccess entityAccess) {
                entityAccess.setProperty(property.name, getValue(oIdentifiable, property))
            }
        }

        @Override
        void marshall(OIdentifiable nativeEntry, Simple property, EntityAccess entityAccess, OrientSession session) {
            def type = property.type
            def converter = SIMPLE_TYPE_CONVERTERS[type]
            if (converter == null) {
                converter = DEFAULT_CONVERTER
            }
            converter.marshall(nativeEntry, property, entityAccess)
        }

        @Override
        void unmarshall(OIdentifiable nativeEntry, Simple property, EntityAccess entityAccess, OrientSession session) {
            def type = property.type
            def converter = SIMPLE_TYPE_CONVERTERS[type]
            if (converter == null) {
                converter = DEFAULT_CONVERTER
            }
            converter.unmarshall(nativeEntry, property, entityAccess)
        }
    }

    static class CustomTypeConverter implements PropertyConverter<Custom> {

        @Override
        void marshall(OIdentifiable nativeEntry, Custom property, EntityAccess entityAccess, OrientSession session) {
            throw new IllegalAccessException("Not yet implemented in GORM for OrientDB")
        }

        @Override
        void unmarshall(OIdentifiable nativeEntry, Custom property, EntityAccess entityAccess, OrientSession session) {
            throw new IllegalAccessException("Not yet implemented in GORM for OrientDB")
        }
    }

    static class OneToManyConverter implements PropertyConverter<Association> {

        @Override
        void marshall(OIdentifiable nativeEntry, Association association, EntityAccess entityAccess, OrientSession session) {
            def value = entityAccess.getProperty(association.name)
            if (value != null) {
                def associatedEntity = association.getAssociatedEntity()
                def associationAccess = session.createEntityAccess(associatedEntity, value)
                if (!association.owningSide && association.referencedPropertyName == null) {
                    //def list = session.persist((Iterable) associationAccess.getEntity())
                    //OrientGormHelper.setValue((OrientPersistentEntity) entityAccess.persistentEntity, association, ((OIdentifiable) entityAccess.identifier).record.load(), list)
                    return
                }
                if (!association.owningSide && association.referencedPropertyName != null) {
                   // session.persist((Iterable) associationAccess.getEntity())
                }
            }

        }

        @Override
        void unmarshall(OIdentifiable nativeEntry, Association association, EntityAccess entityAccess, OrientSession session) {
            def entity = entityAccess.entity
            if (!association.owningSide && association.referencedPropertyName == null) {
                //entityAccess.setProperty(association.name, new OrientLinkedSet(entityAccess, session, association, (OIdentifiable) entityAccess.identifier))
                return;
            }
            if (!association.owningSide && association.referencedPropertyName != null) {
                def queryExecutor = (AssociationQueryExecutor) new OrientAssociationQueryExecutor((OIdentifiable) entityAccess.identifier, association, session)
                def associationSet = new OrientPersistentSet((Serializable) entityAccess.identifier, session, entityAccess, (ToMany) association)
                entityAccess.setPropertyNoConversion(association.name, associationSet)
            }
        }

        static initializePersistentCollection(Session session, EntityAccess entityAccess, Association property) {
            def type = property.type
            def propertyName = property.name
            def identifier = (Serializable) entityAccess.identifier

            if (SortedSet.isAssignableFrom(type)) {
                entityAccess.setPropertyNoConversion(
                        propertyName,
                        new PersistentSortedSet(property, identifier, session)
                )
            } else if (Set.isAssignableFrom(type)) {
                entityAccess.setPropertyNoConversion(
                        propertyName,
                        new PersistentSet(property, identifier, session)
                )
            } else {
                entityAccess.setPropertyNoConversion(
                        propertyName,
                        new PersistentList(property, identifier, session)
                )
            }
        }
    }

    /**
     * A {@PropertyEncoder} capable of encoding {@ToOne} association types
     */
    static class ToOneConverter implements PropertyConverter<ToOne> {

        @Override
        void marshall(OIdentifiable nativeEntry, ToOne property, EntityAccess entityAccess, OrientSession session) {
            def value = session.mappingContext.proxyFactory.unwrap(entityAccess.getProperty(property.name))
            if (value) {
                def associatedEntity = property.associatedEntity
                def associationAccess = session.createEntityAccess(associatedEntity, value)
                def parent = ((OIdentifiable) entityAccess.identifier)?.record?.load()
                def child = ((OIdentifiable) associationAccess.identifier)?.record?.load()
                if (property.doesCascade(CascadeType.PERSIST) && associatedEntity != null) {
                    if (!property.isForeignKeyInChild()) {
                        if (!child) {
                            //child = session.getPersister(associatedEntity).persist(value)
                        }
                        setValue((OIdentifiable) parent, property, child)
                        // adding to referenced side collection
                        if (property.referencedPropertyName != null) {
                            def valueFromAssociated = associationAccess.getProperty(property.referencedPropertyName)
                            if (valueFromAssociated instanceof Collection) {
                                valueFromAssociated.add(entityAccess.entity)
                            }
                        }
                    } else {
                        // session.persist(entityAccess.getProperty(property.name))
                    }
                }
            }
        }

        @Override
        void unmarshall(OIdentifiable nativeEntry, ToOne property, EntityAccess entityAccess, OrientSession session) {
            def orientAttribute = (OrientAttribute) property.mapping.mappedForm
            boolean isLazy = isLazyAssociation(orientAttribute)
            def associatedEntity = property.associatedEntity
            if (associatedEntity == null) {
                return;
            }
            if (isLazy) {
                if (property.owningSide && property.foreignKeyInChild) {
                    def queryExecutor = new OrientAssociationQueryExecutor((OIdentifiable) entityAccess.identifier, property, session)
                    //def result = new OrientPersistentSet((Serializable) entityAccess.identifier, session, entityAccess, (ToMany) property)
                   // entityAccess.setProperty(property.name, result);
                    return;
                }
                def value = getValue(nativeEntry, property)
                if (value != null) {
                    def proxy = session.mappingContext.proxyFactory.createProxy((Session) session, associatedEntity.javaClass, ((OIdentifiable) value).identity)
                    entityAccess.setProperty(property.name, proxy)
                }
            } else {
                throw new UnsupportedOperationException("seems that relation should be egerly fetched, not supported")
            }
        }

        private boolean isLazyAssociation(OrientAttribute attribute) {
            if (attribute == null) {
                return true
            }
            return attribute.getFetchStrategy() == FetchType.LAZY
        }
    }

    /**
     * A {@PropertyEncoder} capable of encoding {@Embedded} association types
     */
    static class EmbeddedConverter implements PropertyConverter<Embedded> {

        @Override
        void marshall(OIdentifiable nativeEntry, Embedded property, EntityAccess entityAccess, OrientSession session) {
            throw new IllegalAccessException("Not yet implemented in GORM for OrientDB")
        }

        @Override
        void unmarshall(OIdentifiable nativeEntry, Embedded property, EntityAccess entityAccess, OrientSession session) {
            def value = entityAccess.getProperty(property.name)
            throw new IllegalAccessException("Not yet implemented in GORM for OrientDB")
        }
    }

    /**
     * A {@PropertyEncoder} capable of encoding {@EmbeddedCollection} collection types
     */
    static class EmbeddedCollectionConverter implements PropertyConverter<EmbeddedCollection> {

        @Override
        void marshall(OIdentifiable nativeEntry, EmbeddedCollection property, EntityAccess entityAccess, OrientSession session) {
            throw new IllegalAccessException("Not yet implemented in GORM for OrientDB")
        }

        @Override
        void unmarshall(OIdentifiable nativeEntry, EmbeddedCollection property, EntityAccess entityAccess, OrientSession session) {
            throw new IllegalAccessException("Not yet implemented in GORM for OrientDB")
        }
    }

    static void setValue(OIdentifiable entry, PersistentProperty property, Object value, OType valueType = null) {
        def nativeName = MappingUtils.getTargetKey(property)
        if (entry instanceof ODocument) {
            if (value == null && !entry.containsField(nativeName)) return;
            if (valueType == null) {
                valueType = OType.getTypeByClass(value.class)
            }
            entry.field(nativeName, value, valueType)
        }
        if (entry instanceof OrientElement) {
            if (value == null && !entry.hasProperty(nativeName)) return;
            if (valueType == null) {
                valueType = OType.getTypeByClass(value.class)
            }
            if (OType.LINK && value instanceof OrientElement) {
                entry.setProperty(nativeName, value.record, valueType)
                return;
            }
            entry.setProperty(nativeName, value, valueType)
        }
    }

    static OrientEdge createEdge(OrientSession session, OrientPersistentEntity edgeEntity, OIdentifiable vertexFrom, OIdentifiable vertexTo) {
        def graph = session.graph
        return graph.addEdge("class:$edgeEntity.className", graph.getVertex(vertexTo.identity), graph.getVertex(vertexFrom.identity), edgeEntity.className)
    }

    static Object getValue(OIdentifiable entry, PersistentProperty property, OType type = null) {
        def nativeName = MappingUtils.getTargetKey(property)
        if (entry instanceof ODocument) {
            return entry.field(nativeName, type)
        } else if (entry instanceof OrientElement) {
            return ((ODocument)entry.record).field(nativeName, type)
        }
        return null
    }

    static class EdgeOneToOneConverter implements PropertyConverter<OneToOne> {
        @Override
        void marshall(OIdentifiable nativeEntry, OneToOne property, EntityAccess entityAccess, OrientSession session) {

        }

        @Override
        void unmarshall(OIdentifiable nativeEntry, OneToOne property, EntityAccess entityAccess, OrientSession session) {

        }
    }

    static class EdgeOneToManyConverter implements PropertyConverter<OneToMany> {
        @Override
        void marshall(OIdentifiable nativeEntry, OneToMany property, EntityAccess entityAccess, OrientSession session) {
        }

        @Override
        void unmarshall(OIdentifiable nativeEntry, OneToMany property, EntityAccess entityAccess, OrientSession session) {
           // entityAccess.setProperty(property.name, new OrientPersistentSet((Serializable) entityAccess.identifier, session, new OrientEdgeAssociationQueryExecutor(property, session)))
        }
    }

    static class EdgeManyToManyConverter implements PropertyConverter<ManyToMany> {
        @Override
        void marshall(OIdentifiable nativeEntry, ManyToMany property, EntityAccess entityAccess, OrientSession session) {
            def value = entityAccess.getProperty(property.name)
            if (value != null) {
                def childEntityAccess = session.createEntityAccess(property.associatedEntity, entityAccess.getProperty(property.name))

            }
        }

        @Override
        void unmarshall(OIdentifiable nativeEntry, ManyToMany property, EntityAccess entityAccess, OrientSession session) {
            //entityAccess.setProperty(property.name, new OrientPersistentSet((Serializable) entityAccess.identifier, session, new OrientEdgeAssociationQueryExecutor(property, session, false, null)))
        }
    }

    static class EdgeManyToOneConverter implements PropertyConverter<ManyToOne> {
        @Override
        void marshall(OIdentifiable nativeEntry, ManyToOne property, EntityAccess entityAccess, OrientSession session) {
            def value = entityAccess.getProperty(property.name)
            if (value != null) {
                def childEntityAccess = session.createEntityAccess(property.associatedEntity, value)
                def child = ((OIdentifiable) childEntityAccess.identifier)?.record?.load()
                def parent = ((OIdentifiable) entityAccess.identifier)?.record?.load()
                if (child == null) {
                    //session.getPersister(property.associatedEntity).persist(childEntityAccess.getEntity())
                }
                if (!property.isForeignKeyInChild() && !property.owningSide) {
                    def edgeEntity = session.mappingContext.getPersistentEntity(((OrientAttribute) property.mapping.mappedForm).edge.name)
                    createEdge(session, (OrientPersistentEntity) edgeEntity, (OIdentifiable) entityAccess.identifier, (OIdentifiable) childEntityAccess.identifier)
                    return;
                }
            }
        }

        @Override
        void unmarshall(OIdentifiable nativeEntry, ManyToOne property, EntityAccess entityAccess, OrientSession session) {
            if (!property.isForeignKeyInChild() && !property.owningSide) {
                def queryExecutor = new OrientEdgeAssociationQueryExecutor(property, session, true, (OIdentifiable) entityAccess.identifier) as AssociationQueryExecutor
                final Object proxy = session.getMappingContext().getProxyFactory().createProxy(session, queryExecutor, (Serializable) entityAccess.identifier);
                entityAccess.setProperty(property.name, proxy);
            }
        }
    }

    static class LinkedOneToOneConverter implements PropertyConverter<OneToOne> {
        @Override
        void marshall(OIdentifiable nativeEntry, OneToOne property, EntityAccess entityAccess, OrientSession session) {

        }

        @Override
        void unmarshall(OIdentifiable nativeEntry, OneToOne property, EntityAccess entityAccess, OrientSession session) {

        }
    }

    static class LinkedOneToManyConverter implements PropertyConverter<OneToMany> {
        @Override
        void marshall(OIdentifiable nativeEntry, OneToMany property, EntityAccess entityAccess, OrientSession session) {

        }

        @Override
        void unmarshall(OIdentifiable nativeEntry, OneToMany property, EntityAccess entityAccess, OrientSession session) {

        }
    }

    static class LinkedManyToManyConverter implements PropertyConverter<ManyToMany> {
        @Override
        void marshall(OIdentifiable nativeEntry, ManyToMany property, EntityAccess entityAccess, OrientSession session) {

        }

        @Override
        void unmarshall(OIdentifiable nativeEntry, ManyToMany property, EntityAccess entityAccess, OrientSession session) {

        }
    }

    static class LinkedManyToOneConverter implements PropertyConverter<ManyToOne> {
        @Override
        void marshall(OIdentifiable nativeEntry, ManyToOne property, EntityAccess entityAccess, OrientSession session) {

        }

        @Override
        void unmarshall(OIdentifiable nativeEntry, ManyToOne property, EntityAccess entityAccess, OrientSession session) {

        }
    }

    static interface PropertyConverter<T extends PersistentProperty> {
        void marshall(OIdentifiable nativeEntry, T property, EntityAccess entityAccess, OrientSession session)

        void unmarshall(OIdentifiable nativeEntry, T property, EntityAccess entityAccess, OrientSession session)
    }

    static interface SimpleTypeConverter {
        void marshall(OIdentifiable oIdentifiable, Simple property, EntityAccess entityAccess)

        void unmarshall(OIdentifiable oIdentifiable, Simple property, EntityAccess entityAccess)
    }
}
