package app.cbo.oidc.java.server.scan.sample;

import app.cbo.oidc.java.server.scan.sample.sub.Interface;

import java.util.List;

public record Root(AlsoNeedsBackend alsoNeedsBackend,
                   List<Child> children,
                   NeedsBackend needsBackend,
                   Interface anInterface) {
}
