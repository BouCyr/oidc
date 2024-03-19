package app.cbo.oidc.java.server.json;

import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.utils.ReflectionUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static app.cbo.oidc.java.server.utils.ReflectionUtils.NameAndValue;
import static app.cbo.oidc.java.server.utils.ReflectionUtils.toNameAndValue;
import static java.lang.System.lineSeparator;

/**
 * transforms (badly) an object to a JSON representation
 */
class JSONWriter {

    /**
     * Writes an object in JSON form (sufficient impl for my need) with line breaks and indentation
     * <p>
     * will print ALL methods without parameters that does not return void
     * field names will be the method name (with the 'get' suffix removed if present)
     * <p>
     * Numbers and String are OK ; pretty sure other base type will do something weird
     * Collections will be written as a json array
     * Maps will be written as sub objects, with the key as field names. If key is not a string, it will probably crash
     * array are not supported, and will probably crash
     * not tested with java inheritance, but reflection magic may cause crash
     * <p>
     * i.e. do not reuse this code :)
     */
    public static String writeIndented(Object o) {
        String result = write(o, System::lineSeparator);

        //indentation
        //not perfect, but better thant nothing
        //the JSON is copied line-by-line
        //if the line contains '{' or '[', all subsequent lines will have more indentation
        //if the line contains '}' or ']', THIS LINE and all subsequent lines will have less indentation
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
     * Writes an object in JSON form (sufficient impl for my need) with line breaks and indentation
     * <p>
     * will print ALL methods without parameters that does not return void
     * field names will be the method name (with the 'get' suffix removed if present)
     * <p>
     * Numbers and String are OK ; pretty sure other base type will do something weird
     * Collections will be written as a json array
     * Maps will be written as sub objects, with the key as field names. If key is not a string, it will probably crash
     * array are not supported, and will probably crash
     * not tested with java inheritance, but reflection magic may cause crash
     * <p>
     * i.e. do not reuse this code :)
     */
    public static String write(Object o, Supplier<String> breakSupplier) {
        var buffer = new StringBuilder();
        buffer.append("{").append(breakSupplier.get());

        //find all getters & record accessors
        //we create a new ArrayList ; we need to be sure sure list is mutable, since we are going to add a few more lines
        var lines = new ArrayList<String>();

        if (!(o instanceof Map<?, ?>)) {
            lines.addAll(
                    toNameAndValue(o)
                            .stream()
                            .map(nv -> JSONWriter.toJson(nv, breakSupplier))//magic!
                            .toList());
        }

        if (o instanceof Map<?, ?> map && map.keySet().stream().allMatch(k -> k instanceof String)) {

            Map<String, Object> copyWithStringKeys = new HashMap<>();
            map.forEach((k, v) -> {
                if (k instanceof String s) {
                    copyWithStringKeys.put(s, v);
                } else {
                    throw new JsonProcessingException("Key should be strings ; found a "+k.getClass().getSimpleName());
                }
            });

            writeMap(copyWithStringKeys, lines::add, breakSupplier);
        }

        if (o instanceof WithExtraNode wen) {
            writeMap(wen.extranodes(), lines::add, breakSupplier);
        }

        buffer.append(lines.stream().collect(Collectors.joining(","+breakSupplier.get())));

        buffer.append(breakSupplier.get()).append("}");
        return buffer.toString();

    }

    private static void writeMap(Map<String, Object> map, Consumer<String> lineAdder, Supplier<String> br) {
        map.keySet()
                .stream()
                .map(k -> new NameAndValue(map, k))
                .map(nv -> JSONWriter.toJson(nv, br))
                .forEach(lineAdder);
    }

    /**
     * count the nb of occurences of a char in another String.
     *
     * @param what the char looked for ; PLEASE DO NOT INPUT MORE THAN ONE CHAR :)
     * @param in   the string we are looking into
     * @return the number of occurrences of the char in the in...
     */
    private static int countOccur(String what, String in) {
        //stupid but it works
        return in.length() - in.replaceAll(what, "").length();
    }


    @NotNull
    private static String toJson(@NotNull ReflectionUtils.NameAndValue node, Supplier<String> br) {
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
                .append(value(result, br)); //magic !


        return buffer.toString();
    }

    private static String value(Object result, Supplier<String> br) {
        if (result instanceof Boolean b) {
            return b.toString();
        } else if (result instanceof Number n) {
            return n.toString();
        } else if (result instanceof Character c) {
            return "\"" + c + "\"";
        } else if (result instanceof String s) {
            return "\"" + s.replaceAll("\\R", " ") + "\"";
        } else if (result instanceof Map<?, ?> m) {
            return map(m, br);

        } else if (result instanceof Collection<?> c) {
            return collection(c, br);

        } else if (result.getClass().isArray()) {
            int length = Array.getLength(result);
            Collection<Object> col = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                col.add(Array.get(result, i));
            }
            return collection(col, br);
        } else {
            return write(result, br);
        }
    }

    private static String map(Map<?, ?> m, Supplier<String> br) {
        var buffer = new StringBuilder()
                .append("{").append(br.get());

        buffer.append(
                m.entrySet()
                        .stream()
                        .map(entry -> ("\"" + entry.getKey().toString() + "\"") + ": " + value(entry.getValue(), br))
                        .collect(Collectors.joining(", " + br.get())));
        buffer.append("}");
        return buffer.toString();
    }

    private static String collection(Collection<?> c, Supplier<String> br) {
        var buffer = new StringBuilder()
                .append("[").append(br.get());

        buffer.append(
                c.stream()
                        .map(n -> JSONWriter.value(n, br))
                        .collect(Collectors.joining("," + br.get())));

        buffer.append("]");
        return buffer.toString();
    }




}
