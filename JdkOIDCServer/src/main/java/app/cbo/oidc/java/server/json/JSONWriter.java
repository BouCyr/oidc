package app.cbo.oidc.java.server.json;

import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jsr305.Nullable;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.lang.System.lineSeparator;

/**
 * transforms (badly) an object to a JSON representation
 */
class JSONWriter {

    public static String writeIndented(Object o) {
        String result = write(o);

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

    private static String write(Object o) {
        var buffer = new StringBuilder();
        buffer.append("{").append(lineSeparator());

        //find all getters & record accessors
        List<String> lines = new ArrayList<>(Arrays.stream(o.getClass().getMethods())
                .filter(method -> method.getParameterCount() == 0)
                .filter(method -> !Void.TYPE.equals(method.getReturnType()))
                //remove hashcode, getClass & toString
                .filter(method -> !isFromObject(method.getName()))
                .filter(method -> !isExtraNodes(method.getName()))
                .map(method -> new NameAndValue(method, o))
                .map(JSONWriter::toJson)//magic!
                .toList());

        if (o instanceof WithExtraNode wen) {

            wen.extranodes().keySet()
                    .stream()
                    .map(k -> new NameAndValue(wen.extranodes(), k))
                    .map(JSONWriter::toJson)
                    .forEach(lines::add);


        }

        buffer.append(lines.stream().collect(Collectors.joining("," + lineSeparator())));

        buffer.append(lineSeparator()).append("}");
        return buffer.toString();

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

    private static boolean isExtraNodes(String methodName) {
        return "extranodes".equals(methodName);
    }

    //list the method inherited from Object
    private static boolean isFromObject(@Nullable String methodName) {
        return Set.of("toString", "hashCode", "getClass").contains(methodName);
    }

    private static @NotNull
    String toJson(@NotNull NameAndValue node) {
        var buffer = new StringBuilder();


        var result = node.getValue().get();

        if (result == null) {
            return buffer
                    .append("\"").append(node.name()).append("\"")
                    .append(": null").toString();
        }
        if ((result instanceof Optional<?> opt) && opt.isEmpty()) {
            return buffer
                    .append("\"").append(node.name()).append("\"")
                    .append(": null").toString();
        }
        if (result instanceof Optional<?> opt) {
            result = opt.get();
        }

        buffer
                .append("\"").append(node.name()).append("\"")
                .append(": ")
                .append(value(result)); //magic !


        return buffer.toString();
    }

    private static String value(Object result) {
        if (result instanceof Number n) {
            return n.toString();
        } else if (result instanceof Character c) {
            return "\"" + c + "\"";
        } else if (result instanceof String s) {
            return "\"" + s.replaceAll("\\R", " ") + "\"";
        } else if (result instanceof Map<?, ?> m) {
            return map(m);

        } else if (result instanceof Collection<?> c) {
            return collection(c);

        } else if (result.getClass().isArray()) {
            int length = Array.getLength(result);
            Collection<Object> col = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                col.add(Array.get(result, i));
            }
            return collection(col);
        } else {
            return write(result);
        }
    }

    private static String map(Map<?, ?> m) {
        var buffer = new StringBuilder()
                .append("{").append(lineSeparator());

        buffer.append(
                m.entrySet()
                        .stream()
                        .map(entry -> ("\"" + entry.getKey().toString() + "\"") + ": " + value(entry.getValue()))
                        .collect(Collectors.joining(", " + lineSeparator())));
        buffer.append("}");
        return buffer.toString();
    }

    private static String collection(Collection<?> c) {
        var buffer = new StringBuilder()
                .append("[").append(lineSeparator());

        buffer.append(
                c.stream()
                        .map(JSONWriter::value)
                        .collect(Collectors.joining("," + lineSeparator())));

        buffer.append("]");
        return buffer.toString();
    }

    static class NameAndValue {
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
