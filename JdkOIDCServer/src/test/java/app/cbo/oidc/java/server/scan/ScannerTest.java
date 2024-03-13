package app.cbo.oidc.java.server.scan;

import app.cbo.oidc.java.server.scan.exceptions.DownStreamException;
import app.cbo.oidc.java.server.scan.exceptions.MissingConfiguration;
import app.cbo.oidc.java.server.scan.exceptions.NoConstructorFound;
import app.cbo.oidc.java.server.scan.sample.NoDepchild;
import app.cbo.oidc.java.server.scan.sample.Root;
import app.cbo.oidc.java.server.scan.sample.SubChildWithProps;
import app.cbo.oidc.java.server.scan.sample.sub.ProfiledImpl;
import app.cbo.oidc.java.server.utils.Pair;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScannerTest {
    @Test
    void nominal() throws DownStreamException, IOException {
        var scanner = new Scanner("app.cbo.oidc.java.server.scan.sample");
        scanner.withProperties(
                List.of(
                        Pair.of("property", "given"),
                        Pair.of("otherProperty", "alsoGiven"),
                        Pair.of("unused", "unused")));
        var root = scanner.get(Root.class);

        assertThat(root)
                .isNotNull();

        //both implementaions found
        assertThat(root.children())
                .hasSize(2);
        //@instanceOrder ok
        assertThat(root.children().get(0))
                .isInstanceOf(NoDepchild.class);
        assertThat(root.children().get(1))
                .isInstanceOf(SubChildWithProps.class);


        if (root.children().get(1) instanceof SubChildWithProps cast) {

            //check @Prop
            assertThat(cast.property()).isNotNull().isEqualTo("given");
            assertThat(cast.withDefault()).isNotNull().isEqualTo("default");
            assertThat(cast.withDefaultButGiven()).isNotNull()
                    .isEqualTo("alsoGiven")
                    .isNotEqualTo("default_2");
        } else {
            Assertions.fail("wrong cast");
        }


        assertThat(root.alsoNeedsBackend()).isNotNull();
        assertThat(root.needsBackend()).isNotNull();
        assertThat(root.alsoNeedsBackend().noInterfaceBackend()).isNotNull();
        assertThat(root.needsBackend().noInterfaceBackend()).isNotNull();
        assertThat(root.needsBackend().noInterfaceBackend()).isSameAs(root.alsoNeedsBackend().noInterfaceBackend());
        assertThat(root.needsBackend().noInterfaceBackend().random()).isEqualTo(root.alsoNeedsBackend().noInterfaceBackend().random());

    }

    @Test
    void withProfile() throws IOException, DownStreamException {
        var scanner = new Scanner("profiled", "app.cbo.oidc.java.server.scan.sample");
        scanner.withProperties(
                List.of(
                        Pair.of("property", "given"),
                        Pair.of("otherProperty", "alsoGiven"),
                        Pair.of("unused", "unused")));
        var root = scanner.get(Root.class);
        assertThat(root.anInterface()).isInstanceOf(ProfiledImpl.class);
    }

    @Test
    void MissingProp() throws IOException {
        var scanner = new Scanner("app.cbo.oidc.java.server.scan.sample");

        assertThatThrownBy(() -> scanner.get(SubChildWithProps.class))
                .isInstanceOf(DownStreamException.class)
                .hasRootCauseInstanceOf(MissingConfiguration.class);
    }

    @Test
    void notAComponent() throws IOException {
        var scanner = new Scanner("app.cbo.oidc.java.server.scan.sample");

        assertThatThrownBy(() -> scanner.get(RuntimeException.class))
                .isInstanceOf(NoConstructorFound.class);
    }
}