package org.grails.datastore.gorm.orient.extensions

import com.orientechnologies.orient.core.db.record.OIdentifiable
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument
import com.tinkerpop.blueprints.impls.orient.OrientElement
import groovy.transform.CompileStatic
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FromString
import org.grails.datastore.gorm.orient.OrientPersistentEntity
import org.grails.datastore.gorm.orient.OrientDbSession
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.OneToMany

/**
 * Helper methods for OrientDB GORM
 */
@CompileStatic
abstract class OrientGormHelper {

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
    static void setValue(OrientPersistentEntity entity, PersistentProperty property, OIdentifiable instance, Object value) {
        final def nativeName = entity.getNativePropertyName(property.name)
        def valueToSet = value
        OType orientType
        if (valueToSet instanceof OIdentifiable) {
            valueToSet = valueToSet.record
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
    static Object getValue(OrientPersistentEntity entity, PersistentProperty property, OIdentifiable instance) {
        final def nativeName = entity.getNativePropertyName(property.name)
        if (instance instanceof ODocument) {
            return instance.field(nativeName)
        }
        if (instance instanceof OrientElement) {
            return instance.getProperty(nativeName)
        }
        return null
    }

    static OIdentifiable createNewOrientEntry(OrientPersistentEntity entity, Object object, OrientDbSession session) {
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
            return oIdentifiable.record.save()
    }

    static String getOrientClassName(OIdentifiable oIdentifiable) {
        if (oIdentifiable instanceof ODocument) return oIdentifiable.className;
        if (oIdentifiable instanceof OrientElement) return oIdentifiable.record.className;
        return null
    }

    /**
     * Check if recordId is new or invalid, apply a closure on recordId if needed
     *
     * @param object
     * @param converted
     * @return
     */
    static boolean checkForRecordIds(List object, List converted, @ClosureParams(value = FromString, options = ['com.orientechnologies.orient.core.id.ORecordId']) Closure closure = null) {
        def invalidFound = false
        converted.addAll(object.collect { id ->
            if (id != null) {
                def recordId = createRecordId(id)
                closure?.call(recordId)
                invalidFound = recordId.isValid() && recordId.isNew()
                return recordId
            }
            invalidFound = true
            return null
        })
        return invalidFound
    }
}
