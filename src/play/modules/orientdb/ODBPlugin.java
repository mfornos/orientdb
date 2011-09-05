package play.modules.orientdb;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Properties;

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
import com.orientechnologies.orient.core.db.object.ODatabaseObjectPool;
import com.orientechnologies.orient.core.db.object.ODatabaseObjectTx;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;

/**
 * The Orient DB plugin
 */
public class ODBPlugin extends PlayPlugin {

    public static String url;
    public static String user;
    public static String passwd;

    private OServer server;

    private boolean openInView;
    private boolean oivDocumentDB;
    private boolean oivObjectDB;

    @Override
    public void beforeActionInvocation(Method actionMethod) {
        if (actionMethod.isAnnotationPresent(Transactional.class)) {
            Transactional annotation = actionMethod.getAnnotation(Transactional.class);
            ODB.begin(annotation.type(), annotation.db());
        }
    }

    @Override
    public void beforeInvocation() {
        if (openInView) {
            Injector.inject(new DatabaseSource((oivDocumentDB) ? ODB.openDocumentDB() : null, (oivObjectDB) ? ODB
                    .openObjectDB() : null));
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
            server.shutdown();
            server = null;
            if (Play.mode == Mode.DEV) {
                clearPools();
            }
        }
    }

    @Override
    public List<ApplicationClass> onClassesChange(List<ApplicationClass> modified) {
        clearPools();
        registerEntityClasses();
        return modified;
    }

    @Override
    public void onInvocationException(Throwable e) {
        ODB.rollback();
    }

    @Override
    public void onInvocationSuccess() {
        ODB.commit();
    }

    private void clearPools() {
        ODatabaseObjectPool.global().getPools().clear();
        ODatabaseDocumentPool.global().getPools().clear();
    }

    private void configure() {
        Properties p = Play.configuration;
        url = p.getProperty("odb.url", "memory:temp");
        user = p.getProperty("odb.user", "admin");
        passwd = p.getProperty("odb.password", "admin");
        openInView = Boolean.parseBoolean(p.getProperty("odb.open-in-view", "true"));
        oivDocumentDB = Boolean.parseBoolean(p.getProperty("odb.open-in-view.documentdb", "true"));
        oivObjectDB = Boolean.parseBoolean(p.getProperty("odb.open-in-view.objectdb", "true"));
    }

    private void registerEntityClasses() {
        String modelPackage = Play.configuration.getProperty("odb.entities.package", "models");
        ODatabaseObjectTx db = new ODatabaseObjectTx(url);
        db.open(user, passwd);

        for (ApplicationClass appClass : Play.classes.all()) {
            if (appClass.javaClass.getName().startsWith(modelPackage)) {
                db.getEntityManager().registerEntityClass(appClass.javaClass);
                db.getMetadata().getSchema().createClass(appClass.javaClass.getSimpleName());
            }
            db.getMetadata().getSchema().save();
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
