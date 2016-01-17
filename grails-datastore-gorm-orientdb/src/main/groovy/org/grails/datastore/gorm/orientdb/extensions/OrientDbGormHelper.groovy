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
        if (value instanceof OIdentifiable) {
            if (instance instanceof ODocument) {
                if (!value.identity.isNew()) {
                    instance.field(nativeName, value.identity, OType.LINK)
                } else {
                    instance.field(nativeName, value, OType.LINK)
                }
            }
            if (instance instanceof OrientElement) {
                if (!value.identity.isNew()) {
                    instance.setProperty(nativeName, value.identity, OType.LINK)
                } else {
                    instance.setProperty(nativeName, value, OType.LINK)
                }
            }
            return;
        }
        if (instance instanceof ODocument) {
            if (value == null && !instance.containsField(nativeName)) return;
            instance.field(nativeName, value)
        }
        if (instance instanceof OrientElement) {
            if (value == null && !instance.hasProperty(nativeName)) return;
            instance.setProperty(nativeName, value)
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
            return oIdentifiable.save()
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
