package org.grails.datastore.gorm.orient

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.document.config.Collection
import org.grails.datastore.mapping.model.*
import org.grails.datastore.mapping.model.config.GormMappingConfigurationStrategy
/**
 * OrientDB Mapping Context implementation
 *
 * @author eugenekamenev
 */
@CompileStatic
class OrientMappingContext extends AbstractMappingContext {

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

    @Override
    MappingFactory getMappingFactory() {
        return orientDbGormMappingFactory
    }

    @Override
    MappingConfigurationStrategy getMappingSyntaxStrategy() {
        return mappingSyntaxStrategy
    }
}
