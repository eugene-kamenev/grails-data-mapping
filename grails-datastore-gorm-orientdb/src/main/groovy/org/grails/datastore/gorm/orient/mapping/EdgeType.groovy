package org.grails.datastore.gorm.orient.mapping

import com.orientechnologies.orient.core.id.ORecordId
/**
 * Base edge type, should be extended by custom edges
 *
 * @param <I> direction in vertex entity
 * @param <O> direction out vertex entity
 *
 * @author eugene.kamenev
 */
class EdgeType<I, O> {
    ORecordId id

    private I inVertex
    private O outVertex

    static belongsTo = [I, O]

    static mapping = {
        orient type: 'edge'
    }

    void setIn(I instance) {
        inVertex = instance
    }

    I getIn() {
        inVertex
    }

    O getOut() {
        outVertex
    }

    void setOut(O instance) {
        this.outVertex = instance
    }
}