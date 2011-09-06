package play.modules.orientdb;

import java.util.ArrayList;
import java.util.List;

import play.classloading.ApplicationClasses.ApplicationClass;
import play.exceptions.UnexpectedException;

import com.orientechnologies.orient.core.db.ODatabase;
import com.orientechnologies.orient.core.db.ODatabaseListener;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.graph.OGraphDatabase;
import com.orientechnologies.orient.core.db.graph.OGraphDatabasePool;
import com.orientechnologies.orient.core.db.object.ODatabaseObjectPool;
import com.orientechnologies.orient.core.db.object.ODatabaseObjectTx;
import com.orientechnologies.orient.core.hook.ORecordHook;
import com.orientechnologies.orient.core.tx.OTransaction.TXTYPE;

public class ODB {

    public enum DBTYPE {
        DOCUMENT, OBJECT
    };

    public static final List<ApplicationClass> listeners = new ArrayList<ApplicationClass>();
    public static final List<ApplicationClass> hooks = new ArrayList<ApplicationClass>();

    static final ThreadLocal<ODatabaseObjectTx> localObjectTx = new ThreadLocal<ODatabaseObjectTx>();
    static final ThreadLocal<ODatabaseDocumentTx> localDocumentTx = new ThreadLocal<ODatabaseDocumentTx>();
    static final ThreadLocal<OGraphDatabase> localGraphTx = new ThreadLocal<OGraphDatabase>();

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
            ODatabaseDocumentTx db = ODatabaseDocumentPool.global().acquire(ODBPlugin.url, ODBPlugin.user,
                    ODBPlugin.passwd);
            localDocumentTx.set(db);
            registerListeners(db);
        }
        return localDocumentTx.get();
    }

    public static OGraphDatabase openGraphDB() {
        if (!hasGraphTx()) {
            OGraphDatabase db = OGraphDatabasePool.global().acquire(
                    (ODBPlugin.gurl == null) ? ODBPlugin.url : ODBPlugin.gurl, ODBPlugin.user, ODBPlugin.passwd);
            localGraphTx.set(db);
            registerListeners(db);
        }
        return localGraphTx.get();
    }

    public static ODatabaseObjectTx openObjectDB() {
        if (!hasObjectTx()) {
            ODatabaseObjectTx db = ODatabaseObjectPool.global()
                    .acquire(ODBPlugin.url, ODBPlugin.user, ODBPlugin.passwd);
            localObjectTx.set(db);
            registerListeners(db);
            registerHooks(db);
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

    @SuppressWarnings("unchecked")
    private static <T> T newInstance(ApplicationClass appClass) {
        try {
            return (T) appClass.javaClass.newInstance();
        } catch (IllegalArgumentException e) {
            throw new UnexpectedException(e);
        } catch (SecurityException e) {
            throw new UnexpectedException(e);
        } catch (InstantiationException e) {
            throw new UnexpectedException(e);
        } catch (IllegalAccessException e) {
            throw new UnexpectedException(e);
        }
    }

    // XXX expensive operation, find a better solution
    private static void registerHooks(ODatabaseObjectTx db) {
        for (ApplicationClass hook : hooks) {
            db.registerHook((ORecordHook) newInstance(hook));
        }
    }

    // XXX expensive operation
    private static void registerListeners(ODatabase db) {
        for (ApplicationClass listener : listeners) {
            db.registerListener((ODatabaseListener) newInstance(listener));
        }
    }
}
