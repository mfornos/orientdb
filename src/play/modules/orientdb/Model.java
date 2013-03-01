package play.modules.orientdb;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import com.orientechnologies.orient.object.iterator.OObjectIteratorClass;
import play.Play;
import play.data.binding.BeanWrapper;
import play.data.binding.Binder;
import play.data.binding.ParamNode;
import play.data.validation.Validation;
import play.exceptions.UnexpectedException;
import play.utils.Utils;

import com.orientechnologies.orient.core.exception.ORecordNotFoundException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

public class Model implements play.db.Model {
    /**
     * Prepare a query to find *all* entities.
     *
     * @return An OObjectIterator
     */
    public static <T extends Model> OObjectIteratorClass<T> all() {
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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Deprecated
    public static <T extends Model> T create(Class<?> type, String name, Map<String, String[]> params,
            Annotation[] annotations) {
        ParamNode rootParamNode = ParamNode.convert(params);
        return (T)create(rootParamNode, name, type, annotations);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Model> T create(ParamNode rootParamNode, String name, Class<?> type, Annotation[] annotations) {
        try {
            Constructor c = type.getDeclaredConstructor();
            c.setAccessible(true);
            Object model = c.newInstance();
            return (T) edit(rootParamNode, name, model, annotations);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static OObjectDatabaseTx db() {
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

    @Deprecated
    @SuppressWarnings("unchecked")
    public static <T extends Model> T edit(Object o, String name, Map<String, String[]> params, Annotation[] annotations) {
        ParamNode rootParamNode = ParamNode.convert(params);
        return (T)edit( rootParamNode, name, o, annotations);
    }

    @Deprecated
    @SuppressWarnings("unchecked")
    public <T extends Model> T edit(String name, Map<String, String[]> params) {
        ParamNode rootParamNode = ParamNode.convert(params);
        return (T)edit(rootParamNode, name, this, null);
    }

    @SuppressWarnings("unchecked")
    public <T extends Model> T edit(ParamNode rootParamNode, String name) {
        edit(rootParamNode, name, this, null);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Model> T edit(ParamNode rootParamNode, String name, Object o, Annotation[] annotations) {
        ParamNode paramNode = rootParamNode.getChild(name, true);
        List<ParamNode.RemovedNode> removedNodesList = new ArrayList<ParamNode.RemovedNode>();
        try {
            BeanWrapper bw = new BeanWrapper(o.getClass());
            // Start with relations
            Set<Field> fields = new HashSet<Field>();
            Class clazz = o.getClass();
            while (!clazz.equals(Object.class)) {
                Collections.addAll(fields, clazz.getDeclaredFields());
                clazz = clazz.getSuperclass();
            }
            for (Field field : fields) {
                boolean isEntity = false;
                String relation = null;
                boolean multiple = false;
                //
                if (Model.class.isAssignableFrom(field.getType())) {
                    isEntity = true;
                    relation = field.getType().getName();
                }
                if (Collection.class.isAssignableFrom(field.getType())) {
                    Class fieldType = (Class) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                    if (Model.class.isAssignableFrom(fieldType)) {
                        isEntity = true;
                        relation = fieldType.getName();
                        multiple = true;
                    }
                }

                if (isEntity) {
                    ParamNode fieldParamNode = paramNode.getChild(field.getName(), true);
                    Class<Model> c = (Class<Model>) Play.classloader.loadClass(relation);
                    if (Model.class.isAssignableFrom(c)) {
                        String keyName = Model.Manager.factoryFor(c).keyName();
                        if (multiple && Collection.class.isAssignableFrom(field.getType())) {
                            Collection l = new ArrayList();
                            if (SortedSet.class.isAssignableFrom(field.getType())) {
                                l = new TreeSet();
                            } else if (Set.class.isAssignableFrom(field.getType())) {
                                l = new HashSet();
                            }
                            String[] ids = fieldParamNode.getChild(keyName, true).getValues();
                            if (ids != null) {
                                fieldParamNode.removeChild(keyName, removedNodesList);
                                for (String _id : ids) {
                                    if (_id.equals("")) {
                                        continue;
                                    }
                                    Object param = _id;
                                    try {
                                        if (param instanceof String) {
                                            param = new ORecordId((String) param);
                                        }
                                        Object res = ODB.openObjectDB().load((ORID) param);
                                        l.add(res);
                                    } catch (ORecordNotFoundException e) {
                                        Validation.addError(name + "." + field.getName(), "validation.notFound", _id);
                                    }
                                }
                                bw.set(field.getName(), o, l);
                            }
                        } else {
                            String[] ids = fieldParamNode.getChild(keyName, true).getValues();
                            if (ids != null && ids.length > 0 && !ids[0].equals("")) {
                                Object param = ids[0];
                                try {
                                    if (param instanceof String) {
                                        param = new ORecordId((String) param);
                                    }
                                    Object to = ODB.openObjectDB().load((ORID) param);
                                    edit(paramNode, field.getName(), to, field.getAnnotations());
                                    paramNode.removeChild( field.getName(), removedNodesList);
                                    bw.set(field.getName(), o, to);
                                } catch (ORecordNotFoundException e) {
                                    Validation.addError(name + "." + field.getName(), "validation.notFound", ids[0]);
                                    fieldParamNode.removeChild(keyName, removedNodesList);
                                    if (fieldParamNode.getAllChildren().size()==0) {
                                    // remove the whole node..
                                    paramNode.removeChild( field.getName(), removedNodesList);
                                  }
                                }
                            } else if (ids != null && ids.length > 0 && ids[0].equals("")) {
                                bw.set(field.getName(), o, null);
                                fieldParamNode.removeChild(keyName, removedNodesList);
                            }
                        }
                    }
                }
            }
            ParamNode beanNode = rootParamNode.getChild(name, true);
            Binder.bindBean(beanNode, o, annotations);
            return (T) o;
        } catch (Exception e) {
            throw new UnexpectedException(e);
        } finally {
            ParamNode.restoreRemovedChildren( removedNodesList );
        }
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
        try {
            return (T) db().load(id);
        } catch (ORecordNotFoundException e) {
            return null;
        }
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
