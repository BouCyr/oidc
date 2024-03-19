package app.cbo.oidc.java.server.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JSONTest {

    @Test
    void testFlat() throws IOException {
        var json = JSON.jsonify(new FlatRecord(5, "aString"));
        System.out.println("FLAT");
        System.out.println(json);
        System.out.println();

        ObjectMapper jackson = new ObjectMapper();
        var validation = jackson.reader().readValue(json, FlatRecord.class);

        assertThat(validation).isNotNull();
        assertThat(validation.integer()).isEqualTo(5);
        assertThat(validation.myString()).isEqualTo("aString");

    }

    @Test
    void testFlatWithNullField() throws IOException {
        var json = JSON.jsonify(new FlatRecord(5, null));
        System.out.println("FLAT");
        System.out.println(json);
        System.out.println();

        ObjectMapper jackson = new ObjectMapper();
        var validation = jackson.reader().readValue(json, FlatRecord.class);

        assertThat(validation).isNotNull();
        assertThat(validation.integer()).isEqualTo(5);
        assertThat(validation.myString()).isNull();

    }

    @Test
    void testMaster() throws IOException {
        var json = JSON.jsonify(new MasterRecord(8965.56F, "line1" + System.lineSeparator() + "line2",
                new FlatRecord(5, "aString")));

        System.out.println("EMBEDDED");
        System.out.println(json);
        System.out.println();

        ObjectMapper jackson = new ObjectMapper();
        var validation = jackson.reader().readValue(json, MasterRecord.class);

        assertThat(validation).isNotNull();
        assertThat(validation.integer()).isEqualTo(8965.56F);
        assertThat(validation.sub())
                .isNotNull();

        var firstFR = validation.sub();
        assertThat(firstFR.integer()).isEqualTo(5);
        assertThat(firstFR.myString()).isEqualTo("aString");
    }

    @Test
    void testMasterWithNullSubObject() throws IOException {
        var json = JSON.jsonify(new MasterRecord(8965.56F, "line1" + System.lineSeparator() + "line2",
                null));

        System.out.println("EMBEDDED");
        System.out.println(json);
        System.out.println();

        ObjectMapper jackson = new ObjectMapper();
        var validation = jackson.reader().readValue(json, MasterRecord.class);

        assertThat(validation).isNotNull();
        assertThat(validation.integer()).isEqualTo(8965.56F);
        assertThat(validation.sub())
                .isNull();
    }

    @Test
    void testList() throws IOException {
        var json = JSON.jsonify(
                new WithListRecord(8965.56F,
                        "line1" + System.lineSeparator() + "line2",
                        List.of(
                                new FlatRecord(1, "aString"),
                                new FlatRecord(2, "aString"))));
        System.out.println("LIST");
        System.out.println(json);
        System.out.println();

        ObjectMapper jackson = new ObjectMapper();
        var validation = jackson.reader().readValue(json, WithListRecord.class);

        assertThat(validation).isNotNull();
        assertThat(validation.integer()).isEqualTo(8965.56F);
        assertThat(validation.subs())
                .hasSize(2)
                .element(0)
                .isInstanceOf(FlatRecord.class);

        var firstFR = validation.subs().iterator().next();
        assertThat(firstFR.integer()).isEqualTo(1);
        assertThat(firstFR.myString()).isEqualTo("aString");


    }

    @Test
    void testNullList() throws IOException {
        var json = JSON.jsonify(
                new WithListRecord(8965.56F,
                        "line1" + System.lineSeparator() + "line2",
                        null));
        System.out.println("LIST");
        System.out.println(json);
        System.out.println();

        ObjectMapper jackson = new ObjectMapper();
        var validation = jackson.reader().readValue(json, WithListRecord.class);

        assertThat(validation).isNotNull();
        assertThat(validation.integer()).isEqualTo(8965.56F);
        assertThat(validation.subs())
                .isNull();


    }

    @Test
    void testEmptyList() throws IOException {
        var json = JSON.jsonify(
                new WithListRecord(8965.56F,
                        "line1" + System.lineSeparator() + "line2",
                        Collections.emptyList()));

        System.out.println("EMPTYLIST");
        System.out.println(json);
        System.out.println();

        ObjectMapper jackson = new ObjectMapper();
        var validation = jackson.reader().readValue(json, WithListRecord.class);

        assertThat(validation).isNotNull();
        assertThat(validation.integer()).isEqualTo(8965.56F);
        assertThat(validation.subs())
                .isEmpty();


    }

    @Test
    void testMap() throws IOException {
        var json = JSON.jsonify(
                new WithMapRecord(8965.56F,
                        "line1" + System.lineSeparator() + "line2",
                        Map.of(
                                "one", new FlatRecord(1, "aString"),
                                "two", new FlatRecord(2, "aString"))));

        System.out.println("MAP");
        System.out.println(json);
        System.out.println();

        ObjectMapper jackson = new ObjectMapper();
        var validation = jackson.reader().readValue(json, WithMapRecord.class);

        assertThat(validation).isNotNull();
        assertThat(validation.integer()).isEqualTo(8965.56F);
        assertThat(validation.subs())
                .hasSize(2)
                .extractingByKey("one")
                .isInstanceOf(FlatRecord.class);

        var firstFR = validation.subs().get("one");
        assertThat(firstFR.integer()).isEqualTo(1);
        assertThat(firstFR.myString()).isEqualTo("aString");

    }

    @Test
    void testGetter() throws IOException {
        var json = JSON.jsonify(
                new WithGetter("tralala"));

        System.out.println("GETTER");
        System.out.println(json);
        System.out.println();

        ObjectMapper jackson = new ObjectMapper();
        var validation = jackson.reader().readValue(json, WithGetter.class);

        assertThat(validation).isNotNull();
        assertThat(validation.getPayload()).isEqualTo("tralala");
    }

    @Test
    void testChar() throws IOException {
        var json = (JSON.jsonify(new WithChar('c')));

        ObjectMapper jackson = new ObjectMapper();
        var validation = jackson.reader().readValue(json, WithChar.class);

        assertThat(validation).isNotNull();
        assertThat(validation.payload()).isEqualTo('c');
    }

    @Test
    void testArray() throws IOException {
        var json = (JSON.jsonify(new WithArray(new int[]{1, 2, 3, 4, 5})));

        System.out.println(json);
        ObjectMapper jackson = new ObjectMapper();
        var validation = jackson.reader().readValue(json, WithArray.class);

        assertThat(validation).isNotNull();
        assertThat(validation.ints()).isEqualTo(new int[]{1, 2, 3, 4, 5});
    }

    @Test
    void testListOfString() throws IOException {
        var json = (JSON.jsonify(new WithListOfString(List.of("a", "b", "c"))));

        System.out.println(json);
        ObjectMapper jackson = new ObjectMapper();
        var validation = jackson.reader().readValue(json, WithListOfString.class);

        assertThat(validation).isNotNull();
        assertThat(validation.strings()).isEqualTo(List.of("a", "b", "c"));
    }

    @Test
    void testException() {
        assertThatThrownBy(() -> JSON.jsonify(new Throwing()))
                .isInstanceOf(JsonProcessingException.class);
    }

    @Test
    void testOptionalPresent() throws IOException {
        var test = new WithOptional(4, Optional.of("filled!"));
        var json = JSON.jsonify(test);

        ObjectMapper jackson = new ObjectMapper();
        jackson.registerModule(new Jdk8Module());
        var validation = jackson.reader().readValue(json, WithOptional.class);

        assertThat(validation).isNotNull();
        assertThat(validation.anInt()).isEqualTo(4);
        assertThat(validation.maybeAString())
                .isPresent()
                .get().isEqualTo("filled!");
    }

    @Test
    void testOptionalAbsent() throws IOException {
        var test = new WithOptional(4, Optional.empty());
        var json = JSON.jsonify(test);

        ObjectMapper jackson = new ObjectMapper();
        jackson.registerModule(new Jdk8Module());
        var validation = jackson.reader().readValue(json, WithOptional.class);
        assertThat(validation).isNotNull();
        assertThat(validation.anInt()).isEqualTo(4);
        assertThat(validation.maybeAString())
                .isEmpty();
    }

    public record WithOptional(int anInt, Optional<String> maybeAString) {

    }

    public record WithArray(int[] ints) {

    }

    public record WithChar(char payload) {
    }

    public static class Throwing {

        public String getPayload() {
            throw new IllegalStateException("Dang.");
        }
    }

    public static class WithGetter {
        private String payload = null;

        public WithGetter() {
        }

        public WithGetter(String payload) {
            this.payload = payload;
        }

        public String getPayload() {
            return payload;
        }
    }

    public record FlatRecord(int integer, String myString) {
    }

    public record MasterRecord(float integer, String myString, FlatRecord sub) {
    }

    public record WithListRecord(float integer, String myString, Collection<FlatRecord> subs) {
    }

    public record WithListOfString(Collection<String> strings) {
    }

    public record WithMapRecord(float integer, String myString, Map<String, FlatRecord> subs) {
    }


}