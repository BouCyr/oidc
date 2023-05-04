package app.cbo.oidc.java.server.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class JSONTestExtra {

    @Test
    void nominal() throws IOException {
        //check the jackson handling is OK.
        var jsonified = JSON.jsonify(new JSONTest.FlatRecord(5, "5"));


        Map<String, Object> map = new ObjectMapper().reader().readValue(jsonified, Map.class);

        assertThat(map).hasSize(2)
                .containsKey("integer")
                .containsKey("myString")
                .extractingByKey("integer").isEqualTo(5);

    }

    @Test
    void rootAsMap() throws IOException {

        var wen = new WithExtraNodes("str", 4,
                new HashMap<>());

        wen.extranodes.put("t", "test");
        wen.extranodes.put("tt", 4);
        wen.extranodes.put("ttt", List.of(1, 2, 3, 4));
        wen.extranodes.put("nulll", null);

        var jsonified = JSON.jsonify(wen);
        System.out.println(jsonified);

        Map<String, Object> map = new ObjectMapper().reader().readValue(jsonified, Map.class);
        var mapAssert = assertThat(map).hasSize(6)
                .containsKey("str")
                .containsKey("i")
                .containsKey("t")
                .containsKey("tt")
                .containsKey("ttt")
                .containsKey("nulll");

        mapAssert.extractingByKey("str").isEqualTo("str");
        mapAssert.extractingByKey("i").isEqualTo(4);
        mapAssert.extractingByKey("t").isEqualTo("test");
        mapAssert.extractingByKey("tt").isEqualTo(4);
        mapAssert.extractingByKey("nulll").isNull();

        mapAssert.extractingByKey("ttt").isInstanceOf(List.class);
        assertThat((List<?>) map.get("ttt")).hasSize(4)
                .element(3).isEqualTo(4);


    }

    public static record WithExtraNodes(String str, int i, Map<String, Object> extranodes) implements WithExtraNode {

    }
}
