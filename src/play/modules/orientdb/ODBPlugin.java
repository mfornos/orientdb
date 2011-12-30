package play.modules.orientdb;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.orientechnologies.orient.core.command.OCommandManager;
import com.orientechnologies.orient.core.db.ODatabaseListener;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.db.graph.OGraphDatabasePool;
import com.orientechnologies.orient.core.db.object.ODatabaseObjectPool;
import com.orientechnologies.orient.core.db.object.ODatabaseObjectTx;
import com.orientechnologies.orient.core.exception.ORecordNotFoundException;
import com.orientechnologies.orient.core.hook.ORecordHook;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.serialization.serializer.object.OObjectSerializerHelper;
import com.orientechnologies.orient.core.sql.OCommandExecutorSQLDelegate;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;

/**
 * The Orient DB plugin
 */
// TODO implement getStatus
public class ODBPlugin extends PlayPlugin {

    private static final String PLUGIN_PREFIX = "[ODB] ";
    public static String url;
    public static String gurl;
    public static String user;
    public static String passwd;

    private OServer server;

    private int openInView = 0;
    private boolean stale;

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
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Object bind(String name, Class clazz, java.lang.reflect.Type type, Annotation[] annotations,
            Map<String, String[]> params) {
        if (Model.class.isAssignableFrom(clazz)) {
            String keyName = Model.Manager.factoryFor(clazz).keyName();
            String idKey = name + "." + keyName;
            if (params.containsKey(idKey) && params.get(idKey).length > 0 && params.get(idKey)[0] != null
                    && params.get(idKey)[0].trim().length() > 0) {
                String id = params.get(idKey)[0];
                try {
                    OSQLSynchQuery<Model> query = new OSQLSynchQuery<Model>("from " + clazz.getName() + " o where o."
                            + keyName + " = ?");
                    Object param = play.data.binding.Binder.directBind(name, annotations, id + "", Model.Manager
                            .factoryFor(clazz).keyType());
                    List<Model> res = ODB.openObjectDB().query(query, param);
                    Object o = res.get(0);
                    return Model.edit(o, name, params, annotations);
                } catch (ORecordNotFoundException e) {
                    // ok
                } catch (Exception e) {
                    throw new UnexpectedException(e);
                }
            }
            return Model.create(clazz, name, params, annotations);
        }
        return super.bind(name, clazz, type, annotations, params);
    }

    @Override
    public Object bind(String name, Object o, Map<String, String[]> params) {
        if (o instanceof Model) {
            return Model.edit(o, name, params, null);
        }
        return null;
    }

    @Override
    public void detectChange() {
        if (stale && Play.mode == Mode.DEV)
            throw new RuntimeException("Need reload");
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
    public play.db.Model.Factory modelFactory(Class<? extends play.db.Model> modelClass) {
        if (Model.class.isAssignableFrom(modelClass)) {
            return new OModelLoader(modelClass);
        }
        return null;
    }

    @Override
    public void onApplicationStart() {
        stale = false;

        if (server == null) {
            configure();

            if (url.startsWith("remote")) {
                Orient.instance().registerEngine(new OEngineRemote());
            } else {
                runEmbeddedOrientDB();
            }

            registerEntityClasses();
        }
    }

    @Override
    public void onApplicationStop() {
        if (server != null) {
            if (Play.mode == Mode.DEV) {
                clearReferencesToStaleClasses();
            }
            server.shutdown();
            server = null;
        }
    }

    @Override
    public List<ApplicationClass> onClassesChange(List<ApplicationClass> modified) {
        stale = true;
        return super.onClassesChange(modified);
    }

    @Override
    public void onInvocationException(Throwable e) {
        ODB.rollback();
    }

    @Override
    public void onInvocationSuccess() {
        ODB.commit();
    }

    @Override
    public void onLoad() {
        OCommandManager cmdMan = OCommandManager.instance();
        cmdMan.registerExecutor(OSQLSynchPaginatedQuery.class, OCommandExecutorSQLDelegate.class);
    }

    private void clearReferencesToStaleClasses() {
        Logger.trace(PLUGIN_PREFIX + " Cleaning stale classes");
        try {
            Field classes = OObjectSerializerHelper.class.getDeclaredField("classes");
            classes.setAccessible(true);
            classes.set(null, new HashMap<String, List<Field>>());
        } catch (Exception e) {
            // don't worry
        }
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
        if (Boolean.parseBoolean(p.getProperty("odb.open-in-view.graphdb", "true")))
            openInView |= OIV_GRAPH_DB;
    }

    private void info(String msg, Object... args) {
        Logger.info(PLUGIN_PREFIX + msg, args);
    }

    private void registerEntityClasses() {
        String modelPackage = Play.configuration.getProperty("odb.entities.package", "models");
        ODatabaseObjectTx db = new ODatabaseObjectTx(url);
        db.open(user, passwd);

        info("Registering Entities");
        for (ApplicationClass appClass : Play.classes.all()) {
            Class<?> javaClass = appClass.javaClass;
            if (javaClass.getName().startsWith(modelPackage)) {
                // TODO handle Oversize
                String entityName = javaClass.getSimpleName();
                info("Entity: %s", javaClass.getName());
                db.getEntityManager().registerEntityClass(javaClass);
                OSchema schema = db.getMetadata().getSchema();
                if (!schema.existsClass(entityName)) {
                    info("Schema: %s", entityName);
                    schema.createClass(javaClass);
                    schema.save();
                }
            }
        }
        // TODO filtered by package name?
        info("Registering Database Listeners");
        for (ApplicationClass listener : Play.classes.getAssignableClasses(ODatabaseListener.class)) {
            info("Listener: %s", listener.javaClass.getName());
            ODB.listeners.add(listener);
        }
        info("Registering Record Hooks");
        for (ApplicationClass hook : Play.classes.getAssignableClasses(ORecordHook.class)) {
            info("Hook: %s", hook.javaClass.getName());
            ODB.hooks.add(hook);
        }

        db.close();
    }

    private void runEmbeddedOrientDB() {
        try {
            server = OServerMain.create();
            String cfile = Play.configuration.getProperty("odb.config.file", "/play/modules/orientdb/db.config");
            VirtualFile vconf = Play.getVirtualFile(cfile);
            final File configuration;
            if (vconf == null || !vconf.exists()) {
                configuration = writeConfigFile(cfile);
            } else {
                configuration = vconf.getRealFile();
            }
            info("Starting OrientDB embbeded");
            server.startup(configuration);
            info("OrientDB started");
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    private File writeConfigFile(String cfile) throws FileNotFoundException, IOException {
        File f = new File("db.config");
        if (f.exists())
            return f;

        InputStream in = ODBPlugin.class.getResourceAsStream(cfile);
        OutputStream out = new FileOutputStream(f);
        byte buf[] = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0)
            out.write(buf, 0, len);

        out.close();
        in.close();

        return f;
    }
}
