package org.grails.datastore.gorm.orientdb

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.record.impl.ODocument
import com.tinkerpop.blueprints.impls.orient.OrientGraph
import com.tinkerpop.blueprints.impls.orient.OrientVertex

abstract class OrientDbUtils {

    static OrientVertex createTempVertex(OrientGraph orientGraph, String className, Map persistentProperties, Object object) {
        orientGraph.addTemporaryVertex(className)
    }

    static ODocument createDocument(ODatabaseDocumentTx documentTx, String className, Map<String, ?> persistentProperties) {
        ODocument oDocument = new ODocument(className)
        persistentProperties.each { k, v ->
            oDocument.field(k, v)
        }
        oDocument
    }
}
