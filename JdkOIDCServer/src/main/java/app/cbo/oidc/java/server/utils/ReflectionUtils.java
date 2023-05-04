package app.cbo.oidc.java.server.utils;

import app.cbo.oidc.java.server.json.JsonProcessingException;
import app.cbo.oidc.java.server.jsr305.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class ReflectionUtils {

    private ReflectionUtils() {/*utility class*/}

    public static boolean isExtraNodes(String methodName) {
        return "extranodes".equals(methodName);
    }

    //list the method inherited from Object
    public static boolean isFromObject(@Nullable String methodName) {
        return Set.of("toString", "hashCode", "getClass").contains(methodName);
    }


    public static List<NameAndValue> toNameAndValue(Object o) {
        return Arrays.stream(o.getClass().getMethods())
                .filter(method -> method.getParameterCount() == 0)
                .filter(method -> !Void.TYPE.equals(method.getReturnType()))
                //remove hashcode, getClass & toString
                .filter(method -> !isFromObject(method.getName()))
                .filter(method -> !isExtraNodes(method.getName()))
                .map(method -> new ReflectionUtils.NameAndValue(method, o))
                .toList();
    }

    public static class NameAndValue {
        private final String name;
        private final Supplier<Object> value;

        public NameAndValue(String name, Supplier<Object> value) {
            this.name = name;
            this.value = value;
        }

        public NameAndValue(Map<String, ?> map, String key) {
            this(key, () -> map.get(key));
        }

        public NameAndValue(Method method, Object target) {
            String baseName = method.getName();
            if (baseName.startsWith("get")) {
                baseName = baseName.substring(3);
                name = Character.toLowerCase(baseName.charAt(0)) + baseName.substring(1);
            } else {
                name = baseName;
            }
            this.value = () -> {
                try {
                    return method.invoke(target);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new JsonProcessingException(e);
                }
            };
        }

        public String name() {
            return name;
        }

        public Supplier<Object> getValue() {
            return value;
        }
    }
}
