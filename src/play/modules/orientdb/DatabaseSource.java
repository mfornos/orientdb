package play.modules.orientdb;

import play.inject.BeanSource;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.object.ODatabaseObjectTx;

public class DatabaseSource implements BeanSource {
    private final ODatabaseDocumentTx documentDB;
    private final ODatabaseObjectTx objectDB;

    public DatabaseSource(ODatabaseDocumentTx documentTx, ODatabaseObjectTx objectTx) {
        this.documentDB = documentTx;
        this.objectDB = objectTx;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getBeanOfType(Class<T> clazz) {
        if (ODatabaseObjectTx.class.isAssignableFrom(clazz)) {
            return (T) objectDB;
        } else if (ODatabaseDocumentTx.class.isAssignableFrom(clazz)) {
            return (T) documentDB;
        } else {
            return null;
        }
    }

}
