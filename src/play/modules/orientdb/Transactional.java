package play.modules.orientdb;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import play.modules.orientdb.ODB.DBTYPE;

import com.orientechnologies.orient.core.tx.OTransaction.TXTYPE;

@Target(value = { ElementType.METHOD })
@Retention(value = RetentionPolicy.RUNTIME)
@Inherited
public @interface Transactional {
    public DBTYPE db() default DBTYPE.OBJECT;

    public TXTYPE type() default TXTYPE.OPTIMISTIC;
}
