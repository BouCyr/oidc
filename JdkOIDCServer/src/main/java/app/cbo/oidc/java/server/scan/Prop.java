package app.cbo.oidc.java.server.scan;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Prop {

    String NO_DEFAULT = "NO_DEFAULT";

    String value();

    String or() default NO_DEFAULT;
}
