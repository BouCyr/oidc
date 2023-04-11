package app.cbo.oidc.java.server.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JSONWriterTest {

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
    void testException() throws IOException {
        assertThatThrownBy(() -> JSON.jsonify(new Throwing()))
                .isInstanceOf(JsonProcessingException.class);
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

    static record FlatRecord(int integer, String myString) {
    }

    static record MasterRecord(float integer, String myString, FlatRecord sub) {
    }

    static record WithListRecord(float integer, String myString, Collection<FlatRecord> subs) {
    }

    static record WithMapRecord(float integer, String myString, Map<String, FlatRecord> subs) {
    }


}