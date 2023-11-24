package app.cbo.oidc.java.server.scan;

import app.cbo.oidc.java.server.scan.exceptions.MissingConfiguration;
import app.cbo.oidc.java.server.scan.exceptions.UnknownPropertyType;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Properties {

    private final Map<String, String> configuration = new HashMap<>();
    private final Collection<Mapper<?>> mappers = new ArrayList<>();


    public Properties() {

        this.mappers.add(new Mapper<>(ClassId.of(String.class), s -> s));
        this.mappers.add(new Mapper<>(ClassId.of(Path.class), Path::of));
        this.mappers.add(new Mapper<>(ClassId.of(Integer.class), Integer::parseInt));
        //never seen this before, but somehow works
        this.mappers.add(new Mapper<>(ClassId.of(int.class), Integer::parseInt));
        this.mappers.add(new Mapper<>(ClassId.of(Long.class), Long::parseLong));
        this.mappers.add(new Mapper<>(ClassId.of(long.class), Long::parseLong));
        this.mappers.add(new Mapper<>(ClassId.of(Double.class), Double::parseDouble));
        this.mappers.add(new Mapper<>(ClassId.of(double.class), Double::parseDouble));
        this.mappers.add(new Mapper<>(ClassId.of(Float.class), Float::parseFloat));
        this.mappers.add(new Mapper<>(ClassId.of(float.class), Float::parseFloat));

    }

    public void add(String key, String value) {
        this.configuration.put(key, value);
    }

    public <U> U get(String key, Class<U> u) {

        if (!this.configuration.containsKey(key)) {
            throw new MissingConfiguration(key);
        }

        return this.getMapper(u).convert(this.configuration.get(key));

    }

    private <U> Mapper<U> getMapper(Class<U> target) {
        return (Mapper<U>) this.mappers.stream().filter(m -> m.getTargetClass().equals(ClassId.of(target)))
                .findFirst()
                .orElseThrow(() -> new UnknownPropertyType(target));
    }

    private static class Mapper<U> {

        private final ClassId<U> clss;
        private final Function<String, U> converter;

        public Mapper(ClassId<U> clss, Function<String, U> converter) {
            this.clss = clss;
            this.converter = converter;
        }

        public ClassId<U> getTargetClass() {
            return clss;
        }

        public U convert(String value) {
            return this.converter.apply(value);
        }
    }
}
