package org.grails.datastore.gorm.orientdb.engine

import com.orientechnologies.orient.core.db.record.OIdentifiable
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument
import com.tinkerpop.blueprints.impls.orient.OrientElement
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.orientdb.OrientDbSession
import org.grails.datastore.gorm.orientdb.collection.OrientDbPersistentSet
import org.grails.datastore.gorm.orientdb.mapping.config.OrientAttribute
import org.grails.datastore.mapping.collection.PersistentList
import org.grails.datastore.mapping.collection.PersistentSet
import org.grails.datastore.mapping.collection.PersistentSortedSet
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.engine.internal.MappingUtils
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.*

import javax.persistence.CascadeType
import javax.persistence.FetchType

@CompileStatic
class PersistentPropertyConverter {
    private static final Map<Class, ?> PROPERTY_CONVERTERS = [
            (Identity): new IdentityConverter(),
            (Simple): new SimpleConverter(),
            (Custom): new CustomTypeConverter(),
            (OneToMany) : new OneToManyConverter(),
            (ToOne) : new ToOneConverter(),
            (Embedded) : new EmbeddedConverter(),
            (EmbeddedCollection) : new EmbeddedCollectionConverter()
    ]

    static PropertyConverter get(Class propertyClass) {
        return (PropertyConverter) PROPERTY_CONVERTERS[propertyClass]
    }

    /**
     * A {@link PropertyConverter} capable of decoding the {@link org.grails.datastore.mapping.model.types.Identity}
     */
    static class IdentityConverter implements PropertyConverter<Identity> {

        @Override
        void marshall(OIdentifiable nativeEntry, Identity property, EntityAccess entityAccess, OrientDbSession session) {
            // do nothing here, because nativeEntry will have this property already
        }

        @Override
        void unmarshall(OIdentifiable nativeEntry, Identity property, EntityAccess entityAccess, OrientDbSession session) {
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
        void marshall(OIdentifiable nativeEntry, Simple property, EntityAccess entityAccess, OrientDbSession session) {
            def type = property.type
            def converter = SIMPLE_TYPE_CONVERTERS[type]
            if (converter == null) {
                converter = DEFAULT_CONVERTER
            }
            converter.marshall(nativeEntry, property, entityAccess)
        }

        @Override
        void unmarshall(OIdentifiable nativeEntry, Simple property, EntityAccess entityAccess, OrientDbSession session) {
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
        void marshall(OIdentifiable nativeEntry, Custom property, EntityAccess entityAccess, OrientDbSession session) {
            throw new IllegalAccessException("Not yet implemented in GORM for OrientDB")
        }

        @Override
        void unmarshall(OIdentifiable nativeEntry, Custom property, EntityAccess entityAccess, OrientDbSession session) {
            throw new IllegalAccessException("Not yet implemented in GORM for OrientDB")
        }
    }

    static class OneToManyConverter implements PropertyConverter<Association> {

        @Override
        void marshall(OIdentifiable nativeEntry, Association property, EntityAccess entityAccess, OrientDbSession session) {
            boolean shouldEncodeIds = !property.isBidirectional() || (property instanceof ManyToMany)
            println "should encode ids calculated in OneToMany $shouldEncodeIds"
            /*if(shouldEncodeIds) {
                // if it is unidirectional we encode the values inside the current
                // document, otherwise nothing to do, encoding foreign key stored in inverse side

                def associatedEntity = property.associatedEntity
                def mongoSession = (AbstractMongoSession)datastore.currentSession
                if(value instanceof Collection) {
                    boolean updateCollection = false
                    if((value instanceof DirtyCheckableCollection)) {
                        def persistentCollection = (DirtyCheckableCollection) value
                        updateCollection = persistentCollection.hasChanged()
                    }
                    else {
                        // write new collection
                        updateCollection = true
                    }

                    if(updateCollection) {
                        // update existing collection
                        Collection identifiers = (Collection)mongoSession.getAttribute(parentAccess.entity, "${property}.ids")
                        if(identifiers == null) {
                            def fastClassData = FieldEntityAccess.getOrIntializeReflector(associatedEntity)
                            identifiers = ((Collection)value).collect() {
                                fastClassData.getIdentifier(it)
                            }
                        }
                        writer.writeName MappingUtils.getTargetKey((PersistentProperty)property)
                        def listCodec = datastore.codecRegistry.get(List)

                        def identifierList = identifiers.toList()
                        MongoAttribute attr = (MongoAttribute)property.mapping.mappedForm
                        if(attr?.isReference()) {
                            def collectionName = mongoSession.getCollectionName(property.associatedEntity)
                            identifierList = identifierList.findAll(){ it != null }.collect {
                                new DBRef(collectionName, it)
                            }
                        }
                        listCodec.encode writer, identifierList, encoderContext
                    }
                }
            }*/
        }

        @Override
        void unmarshall(OIdentifiable nativeEntry, Association property, EntityAccess entityAccess, OrientDbSession session) {
            if (property.isBidirectional() && !(property instanceof ManyToMany)) {
                println "we need initialize persistent collection"
                //initializePersistentCollection(session, entityAccess, property)
            } else {
                def type = property.type
                def propertyName = property.name
                def associatedType = property.associatedEntity.javaClass
                if (Set.isAssignableFrom(associatedType)) {
                    // return persistentSet
                } else {
                    // return persistentList
                }
            }
        }

        static initializePersistentCollection(Session session, EntityAccess entityAccess, Association property) {
            def type = property.type
            def propertyName = property.name
            def identifier = (Serializable) entityAccess.identifier

            if(SortedSet.isAssignableFrom(type)) {
                entityAccess.setPropertyNoConversion(
                        propertyName,
                        new PersistentSortedSet( property, identifier, session)
                )
            }
            else if(Set.isAssignableFrom(type)) {
                entityAccess.setPropertyNoConversion(
                        propertyName,
                        new PersistentSet( property, identifier, session)
                )
            }
            else {
                entityAccess.setPropertyNoConversion(
                        propertyName,
                        new PersistentList( property, identifier, session)
                )
            }
        }
    }

    /**
     * A {@PropertyEncoder} capable of encoding {@ToOne} association types
     */
    static class ToOneConverter implements PropertyConverter<ToOne> {

        @Override
        void marshall(OIdentifiable nativeEntry, ToOne property, EntityAccess entityAccess, OrientDbSession session) {
            def value = session.mappingContext.proxyFactory.unwrap(entityAccess.getProperty(property.name))
            if (value) {
                def associatedEntity = property.associatedEntity
                def associationAccess = session.createEntityAccess(associatedEntity, value)
                if (property.doesCascade(CascadeType.PERSIST) && associatedEntity != null) {
                    if (!property.isForeignKeyInChild()) {

                        def nativeAssociation = value['dbInstance']
                        if (!nativeAssociation) {
                            println "nativeAssociation was null $value"
                            nativeAssociation = session.getPersister(associatedEntity).persist(value)
                        }
                        setValue((OIdentifiable) entityAccess.entity['dbInstance'], property, nativeAssociation)
                        // adding to referenced side collection
                        if (property.referencedPropertyName != null) {
                            def valueFromAssociated = associationAccess.getProperty(property.referencedPropertyName)
                            if (valueFromAssociated instanceof Collection) {
                                valueFromAssociated.add(entityAccess.entity)
                            }
                        }
                    } else {
                        session.persist(entityAccess.getProperty(property.name))
                    }
                }
            }
        }

        @Override
        void unmarshall(OIdentifiable nativeEntry, ToOne property, EntityAccess entityAccess, OrientDbSession session) {
            def orientAttribute = (OrientAttribute) property.mapping.mappedForm
            boolean isLazy = isLazyAssociation(orientAttribute)
            def associatedEntity = property.associatedEntity
            if(associatedEntity == null) {
                return;
            }
            if(isLazy) {
                if (property.owningSide && property.foreignKeyInChild) {
                    def queryExecutor = new OrientDbAssociationQueryExecutor((OIdentifiable) entityAccess.identifier, property, session)
                    def result = new OrientDbPersistentSet((Serializable) entityAccess.identifier, session, queryExecutor)
                    entityAccess.setProperty(property.name, result);
                    return;
                }
                def value = getValue(nativeEntry, property)
                if (value != null) {
                    def proxy = session.mappingContext.proxyFactory.createProxy((Session) session, associatedEntity.javaClass, ((OIdentifiable)value).identity)
                    entityAccess.setProperty(property.name, proxy)
                }
            }
            else {
                new UnsupportedOperationException("seems that relation should be egerly fetched, not supported")
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
        void marshall(OIdentifiable nativeEntry, Embedded property, EntityAccess entityAccess, OrientDbSession session) {
            throw new IllegalAccessException("Not yet implemented in GORM for OrientDB")
        }

        @Override
        void unmarshall(OIdentifiable nativeEntry, Embedded property, EntityAccess entityAccess, OrientDbSession session) {
            def value = entityAccess.getProperty(property.name)
            throw new IllegalAccessException("Not yet implemented in GORM for OrientDB")
        }
    }

    /**
     * A {@PropertyEncoder} capable of encoding {@EmbeddedCollection} collection types
     */
    static class EmbeddedCollectionConverter implements PropertyConverter<EmbeddedCollection> {

        @Override
        void marshall(OIdentifiable nativeEntry, EmbeddedCollection property, EntityAccess entityAccess, OrientDbSession session) {
            throw new IllegalAccessException("Not yet implemented in GORM for OrientDB")
        }

        @Override
        void unmarshall(OIdentifiable nativeEntry, EmbeddedCollection property, EntityAccess entityAccess, OrientDbSession session) {
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
    
    static Object getValue(OIdentifiable entry, PersistentProperty property) {
        def nativeName = MappingUtils.getTargetKey(property)
        if (entry instanceof ODocument) {
            return entry.field(nativeName)
        } else if (entry instanceof OrientElement) {
            return entry.getProperty(nativeName)
        }
        return null
    }

    static interface PropertyConverter<T extends PersistentProperty> {
        void marshall(OIdentifiable nativeEntry, T property, EntityAccess entityAccess, OrientDbSession session)
        void unmarshall(OIdentifiable nativeEntry, T property, EntityAccess entityAccess, OrientDbSession session)
    }

    static interface SimpleTypeConverter {
        void marshall(OIdentifiable oIdentifiable, Simple property, EntityAccess entityAccess)
        void unmarshall(OIdentifiable oIdentifiable, Simple property, EntityAccess entityAccess)
    }
}
