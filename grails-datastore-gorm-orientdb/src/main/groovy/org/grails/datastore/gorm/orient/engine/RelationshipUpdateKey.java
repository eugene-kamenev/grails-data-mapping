package org.grails.datastore.gorm.orient.engine;

import org.grails.datastore.mapping.model.types.Association;

import java.io.Serializable;

/**
 * Relationship update key, should be unique for pending insert/update/delete operation
 */
public class RelationshipUpdateKey {
    private final Serializable id;
    private final Association association;

    public RelationshipUpdateKey(Serializable id, Association association) {
        this.id = id;
        this.association = association;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RelationshipUpdateKey that = (RelationshipUpdateKey) o;

        return association != null ? association.equals(that.association) : that.association == null && !(id != null ? !id.equals(that.id) : that.id != null);

    }

    @Override
    public int hashCode() {
        int result = association != null ? association.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }
}
