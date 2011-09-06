package play.modules.orientdb;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Properties;

import play.Logger;
import play.Play;
import play.Play.Mode;
import play.PlayPlugin;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.exceptions.UnexpectedException;
import play.inject.Injector;
import play.vfs.VirtualFile;

import com.orientechnologies.orient.client.remote.OEngineRemote;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.db.graph.OGraphDatabasePool;
import com.orientechnologies.orient.core.db.object.ODatabaseObjectPool;
import com.orientechnologies.orient.core.db.object.ODatabaseObjectTx;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;

/**
 * The Orient DB plugin
 */
public class ODBPlugin extends PlayPlugin {

    public static String url;
    public static String gurl;
    public static String user;
    public static String passwd;

    private OServer server;

    private int openInView = 0;

    static final int OIV_DOCUMENT_DB = 1;
    static final int OIV_OBJECT_DB = 2;
    static final int OIV_GRAPH_DB = 4; // 8, 16

    @Override
    public void beforeActionInvocation(Method actionMethod) {
        if (actionMethod.isAnnotationPresent(Transactional.class)) {
            Transactional annotation = actionMethod.getAnnotation(Transactional.class);
            ODB.begin(annotation.type(), annotation.db());
        }
    }

    @Override
    public void beforeInvocation() {
        if (openInView != 0) {
            Injector.inject(new DatabaseSource(openInView));
        }
    }

    @Override
    public void enhance(ApplicationClass applicationClass) throws Exception {
        new ODBEnhancer().enhanceThisClass(applicationClass);
    }

    @Override
    public void invocationFinally() {
        ODB.close();
    }

    @Override
    public void onApplicationStart() {
        if (server == null) {
            configure();

            if (url.startsWith("remote")) {
                Orient.instance().registerEngine(new OEngineRemote());
            } else {
                runEmbedOrientDB();
            }
            registerEntityClasses();
        }
    }

    @Override
    public void onApplicationStop() {
        if (server != null) {
            if (Play.mode == Mode.DEV) {
                clearPools();
            } else {
                server.shutdown();
                server = null;
            }
        }
    }

    // @Override
    // public List<ApplicationClass> onClassesChange(List<ApplicationClass>
    // modified) {
    // // TODO impl?
    // return modified;
    // }

    @Override
    public void onInvocationException(Throwable e) {
        ODB.rollback();
    }

    @Override
    public void onInvocationSuccess() {
        ODB.commit();
    }

    private void clearPools() {
        try {
            ODatabaseObjectPool.global().getPools().clear();
        } catch (Exception e) {
            // don't worry
        }
        try {
            ODatabaseDocumentPool.global().getPools().clear();
        } catch (Exception e) {
            // don't worry
        }
        try {
            OGraphDatabasePool.global().getPools().clear();
        } catch (Exception e) {
            // don't worry
        }
    }

    private void configure() {
        Properties p = Play.configuration;
        url = p.getProperty("odb.url", "memory:temp");
        user = p.getProperty("odb.user", "admin");
        passwd = p.getProperty("odb.password", "admin");
        gurl = p.getProperty("odb.graph.url", null);
        if (Boolean.parseBoolean(p.getProperty("odb.open-in-view.documentdb", "true")))
            openInView |= OIV_DOCUMENT_DB;
        if (Boolean.parseBoolean(p.getProperty("odb.open-in-view.objectdb", "true")))
            openInView |= OIV_OBJECT_DB;
        if (Boolean.parseBoolean(p.getProperty("odb.open-in-view.graphdb", "false")))
            openInView |= OIV_GRAPH_DB;
    }

    private void registerEntityClasses() {

        String modelPackage = Play.configuration.getProperty("odb.entities.package", "models");
        ODatabaseObjectTx db = new ODatabaseObjectTx(url);
        db.open(user, passwd);

        for (Class<?> javaClass : Play.classloader.getAllClasses()) {
            if (javaClass.getName().startsWith(modelPackage)) {
                // TODO handle Oversize
                // TODO handle Register hooks
                String entityName = javaClass.getSimpleName();

                if (db.getEntityManager().getEntityClass(entityName) == null) {
                    Logger.trace("ODB registering %s", javaClass.getName());
                    db.getEntityManager().registerEntityClass(javaClass);
                }

                OSchema schema = db.getMetadata().getSchema();
                if (!schema.existsClass(entityName)) {
                    Logger.trace("ODB creating empty schema for %s", entityName);
                    schema.createClass(entityName);
                    schema.save();
                }
            }
        }

        db.close();
    }

    private void runEmbedOrientDB() {
        try {
            server = OServerMain.create();
            String cfile = Play.configuration.getProperty("odb.config.file", "/play/modules/orientdb/db.config");
            VirtualFile vconf = Play.getVirtualFile(cfile);
            final File configuration;
            if (vconf == null || !vconf.exists()) {
                configuration = new File(ODBPlugin.class.getResource(cfile).toURI());
            } else {
                configuration = vconf.getRealFile();
            }
            server.startup(configuration);
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }
}
