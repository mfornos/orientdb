package play.modules.orientdb;

import javassist.CtClass;
import javassist.CtMethod;
import play.Logger;
import play.classloading.ApplicationClasses.ApplicationClass;

public class ODBEnhancer extends play.classloading.enhancers.Enhancer {

    @Override
    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {
        CtClass ctClass = makeClass(applicationClass);

        if (ctClass.subtypeOf(classPool.get(Model.class.getName()))) {
            String entityName = ctClass.getName();

            Logger.trace("Enhancing ODB entity %s", entityName);

            // All
            CtMethod all = CtMethod
                    .make(String
                            .format("public static com.orientechnologies.orient.core.iterator.OObjectIteratorMultiCluster all() { return db().browseClass(play.Play.classloader.getClassIgnoreCase(\"%s\")); }",
                                    entityName), ctClass);
            ctClass.addMethod(all);

            // Count
            CtMethod count = CtMethod
                    .make(String
                            .format("public static long count() { return db().countClass(play.Play.classloader.getClassIgnoreCase(\"%s\")); }",
                                    entityName), ctClass);
            ctClass.addMethod(count);

            // Done.
            applicationClass.enhancedByteCode = ctClass.toBytecode();
            ctClass.defrost();
        }
    }

}
