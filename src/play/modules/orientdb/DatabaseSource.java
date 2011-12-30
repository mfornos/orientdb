package play.modules.orientdb;

import play.inject.BeanSource;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.graph.OGraphDatabase;
import com.orientechnologies.orient.core.db.object.ODatabaseObjectTx;

public class DatabaseSource implements BeanSource {
    private final ODatabaseDocumentTx documentDB;
    private final ODatabaseObjectTx objectDB;
    private final OGraphDatabase graphDB;

    public DatabaseSource(int conf) {
        this.documentDB = isEnabled(conf, ODBPlugin.OIV_DOCUMENT_DB) ? ODB.openDocumentDB() : null;
        this.objectDB = isEnabled(conf, ODBPlugin.OIV_OBJECT_DB) ? ODB.openObjectDB() : null;
        this.graphDB = isEnabled(conf, ODBPlugin.OIV_GRAPH_DB) ? ODB.openGraphDB() : null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getBeanOfType(Class<T> clazz) {
        if (ODatabaseObjectTx.class.isAssignableFrom(clazz)) {
            return (T) objectDB;
        } else if (OGraphDatabase.class.isAssignableFrom(clazz)) {
            return (T) graphDB;
        } else if (ODatabaseDocumentTx.class.isAssignableFrom(clazz)) {
            return (T) documentDB;
        } else {
            return null;
        }
    }

    private boolean isEnabled(int conf, int property) {
        return ((conf & property) == property);
    }

}
