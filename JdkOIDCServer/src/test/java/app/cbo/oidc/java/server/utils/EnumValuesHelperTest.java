package app.cbo.oidc.java.server.utils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

class EnumValuesHelperTest {


    enum TestEnum implements EnumValuesHelper.ParamEnum{
        A,B,C;

        @Override
        public String paramValue() {
            return this.name();
        }
    }
    @Test
    void fromParam() {
        var t = EnumValuesHelper.fromParam("A", TestEnum.values());
        Assertions.assertThat(t).isPresent()
                .get()
                .isEqualTo(TestEnum.A);

        var u = EnumValuesHelper.fromParam("$", TestEnum.values());
        Assertions.assertThat(u).isEmpty();

        var v = EnumValuesHelper.fromParam("", TestEnum.values());
        Assertions.assertThat(v).isEmpty();

        var w = EnumValuesHelper.fromParam(null, TestEnum.values());
        Assertions.assertThat(w).isEmpty();
    }

    @Test
    void fromParams() {
        var t = EnumValuesHelper.fromParams(List.of("A"), TestEnum.values());
        Assertions.assertThat(t)
                .hasSize(1)
                .element(0).isEqualTo(TestEnum.A);

        var u = EnumValuesHelper.fromParams(List.of("A","B"), TestEnum.values());
        var listAssert = Assertions.assertThat(u)
                .hasSize(2);
        listAssert.element(0).isEqualTo(TestEnum.A);
        listAssert.element(1).isEqualTo(TestEnum.B);

        u = EnumValuesHelper.fromParams(List.of("A","B","$"), TestEnum.values());
        listAssert = Assertions.assertThat(u)
                .hasSize(2);
        listAssert.element(0).isEqualTo(TestEnum.A);
        listAssert.element(1).isEqualTo(TestEnum.B);

        var v = EnumValuesHelper.fromParams(Collections.emptyList(), TestEnum.values());
        Assertions.assertThat(v).isEmpty();
        v = EnumValuesHelper.fromParams(null, TestEnum.values());
        Assertions.assertThat(v).isEmpty();
    }
}