package org.grails.datastore.gorm.orient.mapping

import com.orientechnologies.orient.core.id.ORecordId
/**
 * Base edge type, should be extended by custom edges
 *
 * @param <IN> direction in vertex entity
 * @param <OUT> direction out vertex entity
 *
 * @author eugenekamenev
 */
class EdgeType<IN, OUT> {
    ORecordId id

    private IN inVertex
    private OUT outVertex

    static belongsTo = [IN, OUT]

    static mapping = {
        orient type: 'edge'
    }

    void setIn(IN instance) {
        inVertex = instance
    }

    IN getIn() {
        inVertex
    }

    OUT getOut() {
        outVertex
    }

    void setOut(OUT instance) {
        this.outVertex = instance
    }
}