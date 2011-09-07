package play.modules.orientdb;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Id;
import javax.persistence.Transient;
import javax.persistence.Version;

import play.Logger;
import play.db.Model;
import play.db.Model.Factory;
import play.db.Model.Property;
import play.exceptions.UnexpectedException;

import com.orientechnologies.orient.core.annotation.OId;
import com.orientechnologies.orient.core.annotation.OVersion;
import com.orientechnologies.orient.core.db.object.ODatabaseObjectTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.iterator.OObjectIteratorMultiCluster;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

public class OModelLoader implements Factory {
    private final Class<? extends Model> clazz;

    public OModelLoader(Class<? extends play.db.Model> modelClass) {
        this.clazz = modelClass;
    }

    @Override
    public Long count(List<String> searchFields, String keywords, String where) {
        ODatabaseObjectTx db = ODB.openObjectDB();
        String q = "select count(*) from " + clazz.getSimpleName() + " ";
        List<Object> params = new ArrayList<Object>();
        if (keywords != null && !keywords.equals("")) {
            String searchQuery = getSearchQuery(searchFields, params, keywords);
            if (!searchQuery.equals("")) {
                q += " where ( " + searchQuery + " )";
            }
            q += (where != null ? " and " + where : "");
        } else {
            q += (where != null ? " where " + where : "");
        }
        Long count = 0L;
        try {
            List<ODocument> lcount = db.query(new OSQLSynchQuery<Long>(q), params.toArray());
            if (!(lcount == null || lcount.isEmpty())) {
                ODocument doc = lcount.get(0);
                count = doc.field("count");
            }
        } catch (Exception e) {
            Logger.warn(e.getMessage(), e);
        }
        return count;
    }

    @Override
    public void deleteAll() {
        ODatabaseObjectTx db = ODB.openObjectDB();
        db.command(new OCommandSQL("delete from " + clazz.getSimpleName())).execute();
    }

    @Override
    public List<Model> fetch(int offset, int size, String orderBy, String order, List<String> searchFields,
            String keywords, String where) {
        String q = "select from " + clazz.getSimpleName();
        List<Object> params = new ArrayList<Object>();
        if (keywords != null && !keywords.equals("")) {
            String searchQuery = getSearchQuery(searchFields, params, keywords);
            if (!searchQuery.equals("")) {
                q += " where ( " + searchQuery + " )";
            }
            q += (where != null ? " and " + where : "");
        } else {
            q += (where != null ? " where " + where : "");
        }
        if (orderBy == null && order == null) {
            orderBy = "id";
            order = "ASC";
        }
        if (orderBy == null && order != null) {
            orderBy = "id";
        }
        if (order == null || (!order.equals("ASC") && !order.equals("DESC"))) {
            order = "ASC";
        }
        q += " order by " + orderBy + " " + order;

        // XXX
        // Ooops! we can not use Range by ORID :(
        OSQLSynchQuery<Model> query = new OSQLSynchPaginatedQuery<Model>(q, offset, size);
        List<Model> result = ODB.openObjectDB().query(query, params.toArray());
        return result;
    }

    @Override
    public Model findById(Object id) {
        if (id == null) {
            return null;
        }
        if (id instanceof String) {
            id = new ORecordId((String) id);
        } else if (!ORID.class.isAssignableFrom(id.getClass())) {
            throw new UnexpectedException("Please use Object or String for your ids");
        }
        return (Model) ODB.openObjectDB().load((ORID) id);
    }

    public String keyName() {
        return keyField().getName();
    }

    public Class<?> keyType() {
        return keyField().getType();
    }

    public Object keyValue(Model m) {
        try {
            return keyField().get(m);
        } catch (Exception ex) {
            throw new UnexpectedException(ex);
        }
    }

    @Override
    public List<Property> listProperties() {
        List<Model.Property> properties = new ArrayList<Model.Property>();
        Set<Field> fields = new LinkedHashSet<Field>();
        Class<?> tclazz = clazz;
        while (!tclazz.equals(Object.class)) {
            Collections.addAll(fields, tclazz.getDeclaredFields());
            tclazz = tclazz.getSuperclass();
        }
        for (Field f : fields) {
            if (Modifier.isTransient(f.getModifiers())) {
                continue;
            }
            if (f.isAnnotationPresent(Transient.class)) {
                continue;
            }
            Model.Property mp = buildProperty(f);
            if (mp != null) {
                properties.add(mp);
            }
        }
        return properties;
    }

    Model.Property buildProperty(final Field field) {
        Model.Property modelProperty = new Model.Property();
        modelProperty.type = field.getType();
        modelProperty.field = field;
        if (Model.class.isAssignableFrom(field.getType())) {

            modelProperty.isRelation = true;
            modelProperty.relationType = field.getType();
            modelProperty.choices = new Model.Choices() {

                @SuppressWarnings("unchecked")
                public List<Object> list() {
                    return toList((OObjectIteratorMultiCluster<Object>) ODB.openObjectDB().browseClass(field.getType()));
                }
            };
        }
        if (Collection.class.isAssignableFrom(field.getType())) {
            final Class<?> fieldType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
            if (Model.class.isAssignableFrom(fieldType)) {
                modelProperty.isRelation = true;
                modelProperty.isMultiple = true;
                modelProperty.relationType = fieldType;
                modelProperty.choices = new Model.Choices() {

                    @SuppressWarnings("unchecked")
                    public List<Object> list() {
                        return toList((OObjectIteratorMultiCluster<Object>) ODB.openObjectDB().browseClass(fieldType));
                    }
                };

            }
        }
        if (field.getType().isEnum()) {
            modelProperty.choices = new Model.Choices() {

                @SuppressWarnings("unchecked")
                public List<Object> list() {
                    return (List<Object>) Arrays.asList(field.getType().getEnumConstants());
                }
            };
        }
        modelProperty.name = field.getName();
        if (field.getType().equals(String.class)) {
            modelProperty.isSearchable = true;
        }
        if (field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(Version.class)
                || field.isAnnotationPresent(OId.class) || field.isAnnotationPresent(OVersion.class)) {
            modelProperty.isGenerated = true;
        }
        return modelProperty;
    }

    String getSearchQuery(List<String> searchFields, List<Object> params, String keywords) {
        String q = "";
        for (Model.Property property : listProperties()) {
            if (property.isSearchable
                    && (searchFields == null || searchFields.isEmpty() ? true : searchFields.contains(property.name))) {
                if (!q.equals("")) {
                    q += " or ";
                }
                q += property.name + ".toLowerCase() like ?";
                params.add("%" + keywords + "%");
            }
        }
        return q;
    }

    @SuppressWarnings("rawtypes")
    Field keyField() {
        Class c = clazz;
        try {
            while (!c.equals(Object.class)) {
                for (Field field : c.getDeclaredFields()) {
                    if (field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(OId.class)) {
                        field.setAccessible(true);
                        return field;
                    }
                }
                c = c.getSuperclass();
            }
        } catch (Exception e) {
            throw new UnexpectedException("Error while determining the object @Id for an object of type " + clazz);
        }
        throw new UnexpectedException("Cannot get the object @Id for an object of type " + clazz);
    }

    protected List<Object> toList(OObjectIteratorMultiCluster<Object> result) {
        List<Object> list = new ArrayList<Object>();
        for (Object obj : result) {
            list.add(obj);
        }
        return list;
    }
}
