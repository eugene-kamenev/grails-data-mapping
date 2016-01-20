package org.grails.datastore.gorm.orient

import com.orientechnologies.orient.core.db.record.ridbag.ORidBag
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.record.ORecord
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.serialization.OSerializableStream
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.document.config.Collection
import org.grails.datastore.mapping.model.*
import org.grails.datastore.mapping.model.config.GormMappingConfigurationStrategy

@CompileStatic
class OrientMappingContext extends AbstractMappingContext {

    /**
     * As described at http://orientdb.com/docs/latest/Types.html
     */
    private static final Set<String> ORIENT_NATIVE_TYPES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            String.class.getName(),
            Byte.class.getName(),
            Short.class.getName(),
            Integer.class.getName(),
            Long.class.getName(),
            Float.class.getName(),
            Double.class.getName(),
            BigDecimal.class.getName(),
            Date.class.getName(),
            byte[].getClass().getName(),
            ORecordId.class.getName(),
            ORecord.class.getName(),
            ODocument.class.getName(),
            ORidBag.class.getName()
    )));

    protected OrientGormMappingFactory orientDbGormMappingFactory = new OrientGormMappingFactory()
    protected MappingConfigurationStrategy mappingSyntaxStrategy = new GormMappingConfigurationStrategy(mappingFactory);

    OrientMappingContext(Closure defaultMapping) {
        orientDbGormMappingFactory.defaultMapping = defaultMapping
    }

    @Override
    protected PersistentEntity createPersistentEntity(Class javaClass) {
        return createPersistentEntity(javaClass, false);
    }

    @Override
    protected PersistentEntity createPersistentEntity(Class javaClass, boolean external) {
        final OrientPersistentEntity entity = new OrientPersistentEntity(javaClass, this, external);
        return entity;
    }

    @Override
    public PersistentEntity createEmbeddedEntity(Class type) {
        final OrientEmbeddedPersistentEntity embedded = new OrientEmbeddedPersistentEntity(type, this);
        embedded.initialize();
        return embedded;
    }

    static class OrientEmbeddedPersistentEntity extends EmbeddedPersistentEntity {

        private DocumentCollectionMapping classMapping;

        public OrientEmbeddedPersistentEntity(Class type, MappingContext ctx) {
            super(type, ctx);
            classMapping = new DocumentCollectionMapping(this, ctx);
        }

        @Override
        public ClassMapping getMapping() {
            return classMapping;
        }

        public class DocumentCollectionMapping extends AbstractClassMapping<Collection> {
            private Collection mappedForm;

            public DocumentCollectionMapping(PersistentEntity entity, MappingContext context) {
                super(entity, context);
                this.mappedForm = (Collection) context.getMappingFactory().createMappedForm(OrientEmbeddedPersistentEntity.this);
            }
            @Override
            public Collection getMappedForm() {
                return mappedForm;
            }
        }
    }

    /**
     * Check whether a type is a native orientdb type that can be stored without conversion.
     * @param clazz The class to check.
     * @return true if no conversion is required and the type can be stored natively.
     */
    public static boolean isOrientNativeType(Class clazz) {
        return  (  ORIENT_NATIVE_TYPES.contains(clazz.getName())
                || ORecord.class.isAssignableFrom(clazz.getClass())
                || OSerializableStream.class.isAssignableFrom(clazz.getClass())
        );
    }

    @Override
    MappingFactory getMappingFactory() {
        return orientDbGormMappingFactory
    }

    @Override
    MappingConfigurationStrategy getMappingSyntaxStrategy() {
        return mappingSyntaxStrategy
    }
}
