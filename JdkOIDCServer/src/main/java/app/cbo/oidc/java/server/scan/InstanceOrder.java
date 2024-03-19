package app.cbo.oidc.java.server.scan;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * When several implementations are required in a LIST in an @Injectable constructor, this will be used to order them
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface InstanceOrder {
    int value();
}
