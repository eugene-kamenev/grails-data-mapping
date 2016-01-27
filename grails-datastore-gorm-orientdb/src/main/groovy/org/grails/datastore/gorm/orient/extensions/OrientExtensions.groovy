package org.grails.datastore.gorm.orient.extensions

import com.tinkerpop.blueprints.impls.orient.OrientElement
import com.tinkerpop.gremlin.java.GremlinPipeline

/**
 * OrientExtensionMethods
 *
 * @author eugenekamenev
 */
class OrientExtensions {

    /**
    * Start Gremlin pipe query from element
    *
    * @param orientVertex
    * @return
    */
    static GremlinPipeline pipe(OrientElement element) {
        new GremlinPipeline(element)
    }
}
