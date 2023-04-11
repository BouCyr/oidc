package app.cbo.oidc.java.server.json;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.System.lineSeparator;

/**
 * transforms (badly) an object to a JSON representation
 */
class JSONWriter {


    public static String write(Object o) {
        String result = write(o, 0);

        //indentation
        //not perfect, but better thant nothing
        //the JSON is copied line-by-line
        //if the line contains '{' or '[', all subsequent lines will have more identation
        //if the line contains '}' or ']', THIS LINE and all subsequent lines will have less identation
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

    /**
     * count the nb of occurences of a char in another String.
     *
     * @param what the char looked for ; PLEASE DO NOT INPUT MORE THAN ONE CHAR :)
     * @param in   the string we are looking into
     * @return the number of occurences of the char in the in...
     */
    private static int countOccur(String what, String in) {
        //stupid but it works
        return in.length() - in.replaceAll(what, "").length();
    }


    private static String write(Object o, int lvl) {
        var buffer = new StringBuilder();
        buffer.append("{").append(lineSeparator());

        //find all getters & record accessors
        buffer.append(Arrays.stream(o.getClass().getMethods())
                .filter(method -> method.getParameterCount() == 0)
                .filter(method -> !Void.TYPE.equals(method.getReturnType()))
                //remorve hashcode, getClass & toString
                .filter(method -> !isFromObject(method.getName()))
                .map(method -> toJson(lvl + 1, o, method))//magic!
                .collect(Collectors.joining("," + lineSeparator())));

        buffer.append(lineSeparator()).append("}");
        return buffer.toString();

    }

    //list the method inherited from Object
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
                    .append(value(lvl + 1, result)); //magic !

        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new JsonProcessingException(e);
        }
        return buffer.toString();
    }

    private static String value(int lvl, Object result) {
        if (result instanceof Number n) {
            return n.toString();
        } else if (result instanceof Character c) {
            return "\"" + c + "\"";
        } else if (result instanceof String s) {
            return "\"" + s.replaceAll("\\R", " ") + "\"";
        } else if (result instanceof Map<?, ?> m) {
            return map(lvl, m);

        } else if (result instanceof Collection<?> c) {
            return collection(lvl, c);

        } else if (result.getClass().isArray()) {
            int length = Array.getLength(result);
            Collection<Object> col = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                col.add(Array.get(result, i));
            }
            return collection(lvl, col);
        } else {
            return write(result, lvl + 1);
        }
    }

    private static String map(int lvl, Map<?, ?> m) {
        var buffer = new StringBuilder()
                .append("{").append(lineSeparator());

        buffer.append(
                m.entrySet()
                        .stream()
                        .map(entry -> name(entry.getKey().toString()) + ": " + value(lvl + 1, entry.getValue()))
                        .collect(Collectors.joining(", " + lineSeparator())));
        buffer.append("}");
        return buffer.toString();
    }

    private static String collection(int lvl, Collection<?> c) {
        var buffer = new StringBuilder()
                .append("[").append(lineSeparator());

        buffer.append(
                c.stream()
                        .map(i -> JSONWriter.value(lvl + 1, i))
                        .collect(Collectors.joining("," + lineSeparator())));

        buffer.append("]");
        return buffer.toString();
    }


    private static String name(String baseName) {
        String name;
        if (baseName.startsWith("get")) {
            name = baseName.substring(3);
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        } else
            name = baseName;
        return "\"" + name + "\"";
    }
}
