package org.grails.datastore.gorm.orient.mapping

import groovy.transform.CompileStatic
import groovy.transform.Memoized
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.ManyToMany
import org.grails.datastore.mapping.model.types.OneToMany
/**
 * Utility methods for manipulating relationships
 *
 * @author Graeme Rocher
 * @since 5.0
 */
@CompileStatic
class RelationshipUtils {

    @Memoized
    public static boolean useReversedMappingFor(Association association) {
        return association.isBidirectional() &&
                ((association instanceof OneToMany) ||
                        ((association instanceof ManyToMany) && (association.isOwningSide())));
    }

    @Memoized
    public static String relationshipTypeUsedFor(Association association) {
        String name = useReversedMappingFor(association) ?
                association.getReferencedPropertyName() :
                association.getName()
        return name
    }
}
