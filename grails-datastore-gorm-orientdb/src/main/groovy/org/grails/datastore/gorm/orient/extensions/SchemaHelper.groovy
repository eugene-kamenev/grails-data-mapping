package org.grails.datastore.gorm.orient.extensions

import com.orientechnologies.common.util.OCallable
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.metadata.schema.OClass
import com.orientechnologies.orient.core.metadata.schema.OProperty
import com.orientechnologies.orient.core.metadata.schema.OSchema
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.sql.OCommandSQL
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph
import com.tinkerpop.blueprints.impls.orient.OrientGraph
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.orient.OrientPersistentEntity
import org.grails.datastore.gorm.orient.engine.OrientPersistentPropertyConverter
import org.grails.datastore.gorm.orient.mapping.config.OrientAttribute
import org.grails.datastore.mapping.model.types.*

/**
 * OrientDB Schema creation/altering helper methods
 *
 * @author eugenekamenev
 */
@CompileStatic
abstract class SchemaHelper {

    static initDatabase(ODatabaseDocumentTx tx, List<OrientPersistentEntity> entities) {
        def containsGraphs = entities.find {it.graph}
        if (containsGraphs) {
            initGraphDatabaseClasses(tx, entities)
        } else {
            initDocumentDatabaseClasses(tx, entities)
        }
        // creating simple proprties first
        def schema = tx.metadata.schema
        for (entity in entities) {
            def oClass = schema.getClass(entity.className)
            for (p in entity.persistentProperties) {
                def mapping = (OrientAttribute) p.mapping.mappedForm
                if (p instanceof Simple) {
                    def property = getOrCreateProperty(oClass, entity.getNativePropertyName(p.name), p.type)
                    if (mapping.getIndex()) {
                        // here we need a check, but too lazy right now, checking only first
                        def classIndex = oClass.getInvolvedIndexes(entity.getNativePropertyName(p.name))[0]
                        if (!classIndex) {
                            property.createIndex(mapping.getIndex())
                        }
                    }
                }

            }
        }
        // creating links and relations now
        for (entity in entities) {
            if (entity.edge) {
                continue;
            }
            def oClass = schema.getClass(entity.className)
            for (p in entity.persistentProperties) {
                def mapping = (OrientAttribute) p.mapping.mappedForm
                if (p instanceof Association) {
                    def associationEntity = (OrientPersistentEntity) ((Association) p).associatedEntity
                    def associationOClass = schema.getClass(associationEntity.className)
                    if (mapping.edge) {
                        def edgeGormEntity = (OrientPersistentEntity) associationEntity.mappingContext.getPersistentEntity(mapping.edge.name)
                        def inAssociation = (OrientPersistentEntity) ((Association) edgeGormEntity.getPropertyByName('in')).associatedEntity
                        def outAssociation = (OrientPersistentEntity) ((Association) edgeGormEntity.getPropertyByName('out')).associatedEntity
                        def edgeOrientClass = schema.getClass(edgeGormEntity.className)
                        OProperty orientProperty = null
                        if (inAssociation == entity) {
                            orientProperty = getOrCreateLinkedProperty(edgeOrientClass, 'in', OType.LINK, schema.getClass(inAssociation.className))
                        }
                        if(outAssociation == entity) {
                            orientProperty = getOrCreateLinkedProperty(edgeOrientClass, 'out', OType.LINK, schema.getClass(outAssociation.className))
                        }
                        // decide here mapping one-to-many, many-to-one, many-to-many, many-to-one in/out constraints on edges
                        if (p instanceof OneToMany) {

                        }
                        if (p instanceof ManyToOne) {
                            if (inAssociation == entity) {
                                orientProperty.createIndex(OClass.INDEX_TYPE.UNIQUE)
                            }
                        }
                        if (p instanceof OneToOne) {
                            OProperty secondProperty = null
                            if (orientProperty.name == 'in') {
                                secondProperty = getOrCreateLinkedProperty(edgeOrientClass, 'out', OType.LINK, schema.getClass(outAssociation.className))
                            } else {
                                secondProperty = getOrCreateLinkedProperty(edgeOrientClass, 'in', OType.LINK, schema.getClass(inAssociation.className))
                            }
                            orientProperty.createIndex(OClass.INDEX_TYPE.UNIQUE)
                            secondProperty.createIndex(OClass.INDEX_TYPE.UNIQUE)
                        }
                        continue;
                    }
                    if (mapping.type in OrientPersistentPropertyConverter.linkedTypes) {
                        getOrCreateLinkedProperty(oClass, entity.getNativePropertyName(p.name), mapping.type, associationOClass)
                        continue;
                    }
                    // if nothing matched trying to create linked associations
                    if (p instanceof OneToMany) {

                    }
                    if (p instanceof ManyToOne) {
                        if (((ManyToOne) p).owningSide && ((ManyToOne) p).foreignKeyInChild) {
                            continue;
                        }
                    }
                    if (p instanceof ManyToMany) {
                        continue;
                    }
                    if (p instanceof OneToOne) {
                        if (((OneToOne) p).owningSide && !((OneToOne) p).bidirectional) {

                        }
                    }
                }
            }
        }
        if (OrientGraph.activeGraph != null) {
            if (!OrientGraph.activeGraph.isClosed()) {
                OrientGraph.activeGraph.shutdown(false, false)
            }
        }
        if (!tx.isClosed()) {
            tx.close()
        }
    }

    static initGraphDatabaseClasses(ODatabaseDocumentTx tx, List<OrientPersistentEntity> entities) {
        def graph = new OrientGraphNoTx(tx)
        for (entity in entities) {
            graph.executeOutsideTx(new OCallable<Object, OrientBaseGraph>() {
                public Object call(OrientBaseGraph iArgument) {
                    def oClass = getOrCreateClass(tx.metadata.schema, entity.className)
                    if (entity.edge) {
                        oClass.setSuperClasses([tx.metadata.schema.getClass('E')])
                        return
                    }
                    oClass.setSuperClasses([tx.metadata.schema.getClass('V')])
                    return null
                }
            });
        }
        graph.shutdown(false)
    }

    /**
     * Get or create class in OrientDB
     *
     * @param schema
     * @param className
     * @return
     */
    static OClass getOrCreateClass(OSchema schema, String className) {
        def oclass = schema.getClass(className)
        if (!oclass) {
            oclass = schema.createClass(className)
        }
        oclass
    }

    static initDocumentDatabaseClasses(ODatabaseDocumentTx tx, List<OrientPersistentEntity> entities) {
        for (entity in entities) {
            tx.command(new OCommandSQL("CREATE CLASS $entity.className")).execute()
        }
    }

    /**
     * Get or create property in OrientDB
     *
     * @param oClass
     * @param property
     * @param type
     * @return
     */
    static OProperty getOrCreateProperty(OClass oClass, String property, Class type) {
        if (property != '@rid') {
            def prop = oClass.getProperty(property)
            if (!prop && type != MetaClass) {
                prop = oClass.createProperty(property, OType.getTypeByClass(type))
            }
            return prop
        }
        null
    }

    /**
     * Get or create property in OrientDB
     *
     * @param oClass
     * @param property
     * @param type
     * @return
     */
    static OProperty getOrCreateLinkedProperty(OClass oClass, String property, OType type, OClass linkedClass) {
        def prop = oClass.getProperty(property)
        if (prop == null) {
            prop = oClass.createProperty(property, type, linkedClass)
        }
        if (!(prop.type in OrientPersistentPropertyConverter.linkedTypes)) {
            prop.setLinkedType(type)
        }
        return prop
    }

    /**
     * Return index type from string
     *
     * @param indexType
     * @return index type from string
     */
    static OClass.INDEX_TYPE getIndexTypeFromString(String indexType) {
        switch (indexType) {
            case 'dictionary': return OClass.INDEX_TYPE.DICTIONARY; break;
            case 'hashUnique': return OClass.INDEX_TYPE.UNIQUE_HASH_INDEX; break;
            case 'hashNotUnique': return OClass.INDEX_TYPE.NOTUNIQUE_HASH_INDEX; break;
            case 'notUnique': return OClass.INDEX_TYPE.NOTUNIQUE; break;
            case 'fulltext': return OClass.INDEX_TYPE.FULLTEXT; break;
            case 'fulltextHash': return OClass.INDEX_TYPE.FULLTEXT_HASH_INDEX; break;
            case 'unique': return OClass.INDEX_TYPE.UNIQUE; break;
            case 'dictionaryHash': return OClass.INDEX_TYPE.DICTIONARY_HASH_INDEX; break;
            case 'spatial': return OClass.INDEX_TYPE.SPATIAL; break;
            default: return OClass.INDEX_TYPE.UNIQUE; break;
        }
    }
}
