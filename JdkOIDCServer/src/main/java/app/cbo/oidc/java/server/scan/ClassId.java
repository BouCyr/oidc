package app.cbo.oidc.java.server.scan;

import java.util.Objects;

public class ClassId<U> {
    private final String id;


    public ClassId(Class<U> clazz) {
        this.id = clazz.getCanonicalName();
    }

    public static <U> ClassId<U> of(Class<U> implementation) {
        return new ClassId<>(implementation);
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClassId<?> classId = (ClassId<?>) o;

        return Objects.equals(id, classId.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
