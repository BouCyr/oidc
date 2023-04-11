package app.cbo.oidc.java.server.json;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.System.lineSeparator;

class JSONWriter {


    public static String write(Object o) {
        String result = write(o, 0);

        var lines = result.lines().toList();
        int idt = 0;

        StringBuilder indented = new StringBuilder();
        for (var line : lines) {


            idt -= countOccur("}", line);
            idt -= countOccur("]", line);

            indented.append("  ".repeat(idt)).append(line).append(lineSeparator());

            idt += countOccur("\\{", line);
            idt += countOccur("\\[", line);

            if (idt < 0)
                idt = 0;
        }

        return indented.toString();

    }

    public static int countOccur(String what, String in) {
        return in.length() - in.replaceAll(what, "").length();
    }


    static String write(Object o, int lvl) {


        var buffer = new StringBuilder();
        buffer.append("{").append(lineSeparator());


        buffer.append(Arrays.stream(o.getClass().getMethods())
                .filter(method -> method.getParameterCount() == 0)
                .filter(method -> !Void.TYPE.equals(method.getReturnType()))
                .filter(method -> !isFromObject(method.getName()))
                .map(method -> toJson(lvl + 1, o, method))
                .collect(Collectors.joining("," + lineSeparator())));

        buffer.append(lineSeparator()).append("}");
        return buffer.toString();

    }

    private static boolean isFromObject(String methodName) {
        return Set.of("toString", "hashCode", "getClass").contains(methodName);
    }

    private static String toJson(int lvl, Object o, Method method) {
        var buffer = new StringBuilder();
        try {
            var result = method.invoke(o);

            String name = name(method.getName());
            buffer.append(name)
                    .append(": ")
                    .append(value(lvl + 1, result));

        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new JsonProcessingException(e);
        }
        return buffer.toString();
    }

    private static String value(int lvl, Object result) {

        //TODO [11/04/2023] array[]
        if (result instanceof Number n) {
            return n.toString();

        } else if (result instanceof Map<?, ?> m) {
            var buffer = new StringBuilder()
                    .append("{").append(lineSeparator());

            buffer.append(
                    m.entrySet()
                            .stream()
                            .map(entry -> name(entry.getKey().toString()) + ": " + value(lvl + 1, entry.getValue()))
                            .collect(Collectors.joining(", " + lineSeparator())));
            buffer.append("}");
            return buffer.toString();

        } else if (result instanceof Collection<?> c) {
            var buffer = new StringBuilder()
                    .append("[").append(lineSeparator());

            buffer.append(
                    c.stream()
                            .map(i -> JSONWriter.value(lvl + 1, i))
                            .collect(Collectors.joining("," + lineSeparator())));

            buffer.append("]");
            return buffer.toString();

        } else if (result instanceof String s) {
            return "\"" + s.replaceAll("\\R", " ") + "\"";
        } else {
            return write(result, lvl + 1);
        }
    }


    private static String name(String baseName) {
        String name;
        if (baseName.startsWith("get"))
            name = baseName.substring(3);
        else
            name = baseName;
        return "\"" + name + "\"";
    }
}
