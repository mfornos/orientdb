package play.modules.orientdb;

import java.util.List;

import play.data.validation.Validation;

import com.orientechnologies.orient.core.db.object.ODatabaseObjectTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.iterator.OObjectIteratorMultiCluster;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

public class Model implements play.db.Model {
    /**
     * Prepare a query to find *all* entities.
     * 
     * @return An OObjectIterator
     */
    public static <T extends Model> OObjectIteratorMultiCluster<T> all() {
        throw new UnsupportedOperationException("Model not enhanced.");
    }

    /**
     * Count entities
     * 
     * @return number of entities of this class
     */
    public static long count() {
        throw new UnsupportedOperationException("Model not enhanced.");
    }

    public static ODatabaseObjectTx db() {
        return ODB.openObjectDB();
    }

    /**
     * Delete all entities
     * 
     * @return Number of entities deleted
     */
    public static int deleteAll() {
        int i = 0;
        for (Model m : all()) {
            m.delete();
            i++;
        }
        return i;
    }

    /**
     * Prepare a query to find entities.
     * 
     * @param query
     *            OSQL query
     * @param params
     *            Params to bind to the query
     * @return A result set
     */
    public static <T extends Model> List<T> find(String query, Object... params) {
        return db().query(new OSQLSynchQuery<T>(query), params);
    }

    /**
     * Find the entity with the corresponding id.
     * 
     * @param id
     *            The entity id
     * @return The entity
     */
    @SuppressWarnings("unchecked")
    public static <T extends Model> T findById(ORID id) {
        return (T) db().load(id);
    }

    @Override
    public void _delete() {
        db().delete(this);
    }

    @Override
    public Object _key() {
        return db().getIdentity(this);
    }

    @Override
    public void _save() {
        db().save(this);
    }

    /**
     * Delete the entity.
     * 
     * @return The deleted entity.
     */
    @SuppressWarnings("unchecked")
    public <T extends Model> T delete() {
        _delete();
        return (T) this;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if ((this == other)) {
            return true;
        }
        if (!this.getClass().isAssignableFrom(other.getClass())) {
            return false;
        }
        if (this._key() == null) {
            return false;
        }
        return this._key().equals(((Model) other)._key());
    }

    public Object getIdentity() {
        return _key();
    }

    @Override
    public int hashCode() {
        if (this._key() == null) {
            return 0;
        }
        return this._key().hashCode();
    }

    public boolean isManaged() {
        return db().isManaged(this);
    }

    /**
     * Refresh the entity state.
     */
    @SuppressWarnings("unchecked")
    public <T extends Model> T refresh() {
        db().reload(this);
        return (T) this;
    }

    /**
     * store (ie insert) the entity.
     */
    @SuppressWarnings("unchecked")
    public <T extends Model> T save() {
        _save();
        return (T) this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + _key() + "]";
    }

    public boolean validateAndSave() {
        if (Validation.current().valid(this).ok) {
            save();
            return true;
        }
        return false;
    }
}
