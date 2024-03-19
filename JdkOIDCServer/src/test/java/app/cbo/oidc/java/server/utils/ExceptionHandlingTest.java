package app.cbo.oidc.java.server.utils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ExceptionHandlingTest {

    @Test
    void testIt(){

        try{
            this.thrower();
        }catch(Exception e) {
            var stack = ExceptionHandling.getStackTrace(e);
            var lines = stack.lines().toList();
            /*
    java.lang.RuntimeException: BLAM
	at app.cbo.oidc.java.server.utils.ExceptionHandlingTest.thrower(ExceptionHandlingTest.java:28)
	at app.cbo.oidc.java.server.utils.ExceptionHandlingTest.testIt(ExceptionHandlingTest.java:16)
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104)
	at java.base/java.lang.reflect.Method.invoke(Method.java:577)
	at org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
	...
	at com.intellij.rt.junit.JUnitStarter.main(JUnitStarter.java:54)
             */
            Assertions.assertThat(lines.get(0)).contains("RuntimeException").contains("BLAM");
            Assertions.assertThat(lines.get(1)).contains("thrower");
            Assertions.assertThat(lines.get(2)).contains("testIt");

        }
    }

    private void thrower() {
        throw new RuntimeException("BLAM");
    }
}