package app.cbo.oidc.java.server.scan;

import app.cbo.oidc.java.server.scan.exceptions.DownStreamException;
import app.cbo.oidc.java.server.scan.exceptions.InstanciationFails;
import app.cbo.oidc.java.server.scan.exceptions.InvalidDepTree;
import app.cbo.oidc.java.server.scan.exceptions.MissingConfiguration;
import app.cbo.oidc.java.server.scan.exceptions.NoConstructorFound;
import app.cbo.oidc.java.server.scan.exceptions.NoImplementationFound;
import app.cbo.oidc.java.server.scan.exceptions.NoResult;
import app.cbo.oidc.java.server.scan.exceptions.NoSingleResult;
import app.cbo.oidc.java.server.scan.exceptions.TooManyResult;
import app.cbo.oidc.java.server.scan.props.Properties;
import app.cbo.oidc.java.server.utils.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class Scanner {


    private final static Logger LOGGER = Logger.getLogger(Scanner.class.getCanonicalName());

    private final String profile;
    private final app.cbo.oidc.java.server.scan.props.Properties properties = new Properties();

    private final Set<Class<?>> classes;

    private final Map<ClassId<?>, BuildStatus> buildProgress = new HashMap<>();
    private final Map<ClassId<?>, Object> instances = new HashMap<>();


    public Scanner(String profile, String basePackage) throws IOException {
        this.profile = profile;

        this.classes = scanPackage(basePackage);
    }

    public Scanner(String basePackage) throws IOException {
        this(Injectable.DEFAULT, basePackage);
    }


    static Set<Class<?>> scanPackage(String packageName) throws IOException {

        LOGGER.info("Scanning " + packageName);
        InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream(packageName.replaceAll("[.]", "/"));

        Set<Class<?>> classes = new HashSet<>();
        assert stream != null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.endsWith(".class")) {
                    var found = getClass(line, packageName);
                    found.ifPresent(classes::add);

                } else {
                    classes.addAll(scanPackage(packageName + "." + line));
                }
            }
        }
        return classes;
    }

    private static Optional<Class<?>> getClass(String className, String packageName) {
        try {
            return Optional.of(Class.forName(packageName + "." + className.substring(0, className.lastIndexOf('.'))));
        } catch (ClassNotFoundException e) {
            // handle the exception
            return Optional.empty();
        }
    }


    public Scanner withProperties(List<Pair<String, String>> properties) {
        properties.forEach(pair -> this.properties.add(pair.left(), pair.right()));
        return this;
    }

    public <T> T get(Class<T> t) throws DownStreamException {

        Class<T> implementation;
        if (!t.isInterface()) {
            implementation = t;
        } else {
            implementation = this.findImplementation(t);
        }

        var prev = this.buildProgress.get(ClassId.of(implementation));
        if (prev == BuildStatus.BUILT) {
            return (T) this.instances.get(ClassId.of(implementation));
        }

        this.buildProgress.put(ClassId.of(implementation), BuildStatus.BUILDING);

        Constructor<T> constructor = this.findConstructor(implementation);

        List<Object> args = new ArrayList<>();

        for (int i = 0; i < constructor.getParameterCount(); i++) {

            var constrArg = constructor.getParameterTypes()[i];
            var constrArgAnnotations = constructor.getParameterAnnotations()[i];


            var propertyKey = getPropertyKey(constrArgAnnotations);
            try {
                if (propertyKey.isPresent()) {
                    //arg is a property
                    var found = this.properties.get(propertyKey.get(), constrArg);
                    if (found.isEmpty()) {
                        found = this.getPropertyDefault(constrArgAnnotations);
                    }
                    if (found.isEmpty()) {
                        throw new MissingConfiguration(propertyKey.get());
                    }
                    args.add(found.get());
                } else if (constructor.getGenericParameterTypes()[i] instanceof ParameterizedType generic
                        && Collection.class.isAssignableFrom(constrArg)) {

                    //constrArg is a collection, but could constructor can expect a Set, a Queue, a List...
                    Collection<Object> arg = this.createCollection(constrArg);


                    for (var c : this.findImplementations(generic.getActualTypeArguments()[0])) {
                        var impl = this.get(c);
                        arg.add(impl);
                    }
                    args.add(arg);
                } else {
                    //arg is a single instance
                    args.add(this.get(constrArg));

                }
            } catch (InvalidDepTree e) {
                throw new DownStreamException(t, e);
            } catch (DownStreamException e) {
                throw new DownStreamException(t, e);
            }
        }


        if (args.size() != constructor.getParameterCount()) {
            LOGGER.severe("Number of built args does not match expected number of args of the constructor");
            //will throw exception next line anyway
        }

        T built;

        try {
            built = constructor.newInstance(args.toArray());
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new InstanciationFails(t, e);
        }
        this.buildProgress.put(ClassId.of(implementation), BuildStatus.BUILT);
        this.instances.put(ClassId.of(implementation), built);
        return built;
    }

    private Collection<Object> createCollection(Class<?> constrArg) {

        if (constrArg == Set.class || constrArg == HashSet.class) {
            return new HashSet<>();
        }
        if (constrArg == List.class || constrArg == ArrayList.class) {
            return new ArrayList<>();
        }
        if (constrArg == Queue.class || constrArg == Deque.class || constrArg == LinkedList.class) {
            return new LinkedList<>();
        }
        throw new NoImplementationFound(constrArg, new IllegalArgumentException("Collection is not a supported type : " + constrArg.getCanonicalName()));
    }

    private Collection<Class<?>> findImplementations(Type actualTypeArgument) {

        if (actualTypeArgument instanceof Class<?> t) {

            return this.classes
                    .stream()
                    .filter(c -> !c.equals(t))
                    .filter(c -> !c.isInterface())
                    .filter(t::isAssignableFrom)
                    .filter(c -> c.isAnnotationPresent(Injectable.class))
                    .sorted(Comparator.comparing(c -> {
                        if (c.isAnnotationPresent(InstanceOrder.class)) {
                            return c.getAnnotation(InstanceOrder.class).value();
                        } else {
                            return Integer.MAX_VALUE;
                        }
                    }))
                    .toList();
        } else {
            //[24/11/2023] no idea.
            throw new RuntimeException("WTF?");
        }
    }

    private Optional<String> getPropertyKey(Annotation[] annotations) {

        return Stream.of(annotations)
                .filter(a -> a.annotationType() == Prop.class)
                .map(a -> (Prop) a)
                .map(Prop::value)
                .findFirst();

    }

    private Optional<String> getPropertyDefault(Annotation[] annotations) {

        var defaultValue = Stream.of(annotations)
                .filter(a -> a.annotationType() == Prop.class)
                .map(a -> (Prop) a)
                .map(Prop::or)
                .findFirst();

        if (defaultValue.isEmpty() || Prop.NO_DEFAULT.equals(defaultValue.get())) {
            return Optional.empty();
        }
        return defaultValue;

    }

    <T> Class<T> findImplementation(Class<T> t) throws NoImplementationFound {

        var implementations = this.classes
                .stream()
                .filter(c -> !c.equals(t))
                .filter(c -> !c.isInterface())
                .filter(t::isAssignableFrom)
                .filter(c -> c.isAnnotationPresent(Injectable.class))
                .toList();

        LOGGER.fine(implementations.size() + " implementations found for " + t.getCanonicalName());

        //is there an impl for the profile ?
        try {
            final Class<?> forThisProfile = ScanUtils.oneAndOnlyOne(implementations
                    .stream()
                    .filter(c -> c.getAnnotation(Injectable.class).value().equals(this.profile))
                    .toList());
            return (Class<T>) forThisProfile;

        } catch (NoResult noResult) {
            //nothing
        } catch (TooManyResult tooManyResult) {
            throw new NoImplementationFound(t, tooManyResult);
        }

        //is there a default impl ?
        try {
            final Class<?> forThisProfile = ScanUtils.oneAndOnlyOne(implementations
                    .stream()
                    .filter(c -> c.getAnnotation(Injectable.class).value().equals(Injectable.DEFAULT))
                    .toList());
            return (Class<T>) forThisProfile;
        } catch (NoSingleResult noResult) {
            throw new NoImplementationFound(t, noResult);
        }

    }

    <T> Constructor<T> findConstructor(Class<T> c) throws NoConstructorFound {

        if (c.getDeclaredConstructors().length == 1) {
            return (Constructor<T>) c.getDeclaredConstructors()[0];
        }

        var constructors = Stream.of(c.getDeclaredConstructors())
                .filter(constructor -> constructor.isAnnotationPresent(BuildWith.class))
                .toList();

        try {
            return (Constructor<T>) ScanUtils.oneAndOnlyOne(constructors);
        } catch (NoSingleResult noResult) {
            throw new NoConstructorFound(c);
        }
    }
}
