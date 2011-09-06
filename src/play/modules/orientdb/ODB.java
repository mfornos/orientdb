package play.modules.orientdb;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.graph.OGraphDatabase;
import com.orientechnologies.orient.core.db.graph.OGraphDatabasePool;
import com.orientechnologies.orient.core.db.object.ODatabaseObjectPool;
import com.orientechnologies.orient.core.db.object.ODatabaseObjectTx;
import com.orientechnologies.orient.core.tx.OTransaction.TXTYPE;

public class ODB {

    public enum DBTYPE {
        DOCUMENT, OBJECT
    };

    static ThreadLocal<ODatabaseObjectTx> localObjectTx = new ThreadLocal<ODatabaseObjectTx>();
    static ThreadLocal<ODatabaseDocumentTx> localDocumentTx = new ThreadLocal<ODatabaseDocumentTx>();
    static ThreadLocal<OGraphDatabase> localGraphTx = new ThreadLocal<OGraphDatabase>();

    public static void begin(TXTYPE type, DBTYPE db) {
        if (db == DBTYPE.DOCUMENT) {
            openDocumentDB().begin(type);
        } else {
            openObjectDB().begin(type);
        }
    }

    public static void close() {
        closeDocument();
        closeObject();
        closeGraph();
    }

    public static void closeDocument() {
        if (hasDocumentTx()) {
            localDocumentTx.get().close();
            localDocumentTx.set(null);
        }
    }

    public static void closeGraph() {
        if (hasGraphTx()) {
            localGraphTx.get().close();
            localGraphTx.set(null);
        }
    }

    public static void closeObject() {
        if (hasObjectTx()) {
            localObjectTx.get().close();
            localObjectTx.set(null);
        }
    }

    public static void commit() {
        if (hasObjectTx() && localObjectTx.get().getTransaction().isActive()) {
            localObjectTx.get().commit();
        }
        if (hasDocumentTx() && localDocumentTx.get().getTransaction().isActive()) {
            localDocumentTx.get().commit();
        }
    }

    public static ODatabaseDocumentTx openDocumentDB() {
        if (!hasDocumentTx()) {
            localDocumentTx
                    .set(ODatabaseDocumentPool.global().acquire(ODBPlugin.url, ODBPlugin.user, ODBPlugin.passwd));
        }
        return localDocumentTx.get();
    }

    public static OGraphDatabase openGraphDB() {
        if (!hasGraphTx()) {
            localGraphTx.set(OGraphDatabasePool.global().acquire(
                    (ODBPlugin.gurl == null) ? ODBPlugin.url : ODBPlugin.gurl, ODBPlugin.user, ODBPlugin.passwd));
        }
        return localGraphTx.get();
    }

    public static ODatabaseObjectTx openObjectDB() {
        if (!hasObjectTx()) {
            localObjectTx.set(ODatabaseObjectPool.global().acquire(ODBPlugin.url, ODBPlugin.user, ODBPlugin.passwd));
        }
        return localObjectTx.get();
    }

    public static void rollback() {
        if (hasObjectTx() && localObjectTx.get().getTransaction().isActive()) {
            localObjectTx.get().rollback();
        }
        if (hasDocumentTx() && localDocumentTx.get().getTransaction().isActive()) {
            localDocumentTx.get().rollback();
        }
    }

    private static boolean hasDocumentTx() {
        return localDocumentTx.get() != null;
    }

    private static boolean hasGraphTx() {
        return localGraphTx.get() != null;
    }

    private static boolean hasObjectTx() {
        return localObjectTx.get() != null;
    }
}
