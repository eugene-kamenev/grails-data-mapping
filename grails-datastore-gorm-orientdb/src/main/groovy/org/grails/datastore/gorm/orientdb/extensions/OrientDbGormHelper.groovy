package org.grails.datastore.gorm.orientdb.extensions

import com.orientechnologies.orient.core.db.record.OIdentifiable
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument
import com.tinkerpop.blueprints.impls.orient.OrientEdge
import com.tinkerpop.blueprints.impls.orient.OrientElement
import com.tinkerpop.blueprints.impls.orient.OrientVertex
import org.grails.datastore.gorm.orientdb.OrientDbPersistentEntity
import org.grails.datastore.gorm.orientdb.OrientDbSession
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.OneToMany

/**
 * Helper methods for OrientDB GORM
 */
abstract class OrientDbGormHelper {

    /**
     * Create recordId from provided object
     *
     * @param key
     * @return
     */
    static ORecordId createRecordId(Object key) {
        ORecordId recId = null;

        if (key instanceof ORecordId) {
            recId = (ORecordId) key;
        } else if (key instanceof String) {
            recId = new ORecordId((String) key);
        }
        return recId;
    }

    /**
     * Native value setter
     *
     * TODO: refactor to something more usable and generic, maybe after schema initialization possibility will be added to mapping context
     *
     * @param entity
     * @param property
     * @param instance
     * @param value
     */
    static void setValue(OrientDbPersistentEntity entity, PersistentProperty property, OIdentifiable instance, Object value) {
        final def nativeName = entity.getNativePropertyName(property.name)
        def valueToSet = value
        OType orientType
        if (valueToSet instanceof OIdentifiable) {
            valueToSet = valueToSet.record
            /*// not sure why, but orientdb saves links in different ways so handle this
            if (!(valueToSet instanceof ODocument)) {
                valueToSet = valueToSet.identity
            } else {
                if (!valueToSet.identity.isNew()) {
                    valueToSet = valueToSet.identity
                }
            }*/
            orientType = OType.LINK
        }
        if (valueToSet instanceof Iterable) {
            if (property instanceof OneToMany) {
                orientType = OType.LINKSET
                valueToSet = valueToSet.collect { OIdentifiable val ->
                    val.record
                }.toSet()
            }
        }
        if (instance instanceof ODocument) {
            if (valueToSet == null && !instance.containsField(nativeName)) return;
            instance.field(nativeName, valueToSet, orientType)
        }
        if (instance instanceof OrientElement) {
            if (valueToSet == null && !instance.hasProperty(nativeName)) return;
            instance.setProperty(nativeName, valueToSet, orientType)
        }
    }

    /**
     * Native value getter
     *
     * TODO: refactor to something more usable and generic, maybe after schema initialization possibility will be added to mapping context
     *
     * @param entity
     * @param property
     * @param instance
     * @return
     */
    static Object getValue(OrientDbPersistentEntity entity, PersistentProperty property, OIdentifiable instance) {
        final def nativeName = entity.getNativePropertyName(property.name)
        if (instance instanceof ODocument) {
            return instance.field(nativeName)
        }
        if (instance instanceof OrientElement) {
            return instance.getProperty(nativeName)
        }
        return null
    }

    static OIdentifiable createNewOrientEntry(OrientDbPersistentEntity entity, Object object, OrientDbSession session) {
        OIdentifiable nativeEntry = null
        if (entity.document) {
            nativeEntry = new ODocument(entity.className)
            object['dbInstance'] = nativeEntry
        }
        if (entity.vertex) {
            nativeEntry = session.graph.addTemporaryVertex(entity.className)
            object['dbInstance'] = nativeEntry
        }
        return nativeEntry
    }

    static OIdentifiable saveEntry(OIdentifiable oIdentifiable) {
        if (oIdentifiable instanceof ODocument) {
            return oIdentifiable.record.save()
        }
        if (oIdentifiable instanceof OrientVertex) {
            oIdentifiable.save()
            return oIdentifiable
        }
        if (oIdentifiable instanceof OrientEdge) {
            oIdentifiable.save()
            return oIdentifiable
        }
        return null
    }

    static String getOrientClassName(OIdentifiable oIdentifiable) {
        if (oIdentifiable instanceof ODocument) return oIdentifiable.className;
        if (oIdentifiable instanceof OrientElement) return oIdentifiable.record.className;
        return null
    }
}
