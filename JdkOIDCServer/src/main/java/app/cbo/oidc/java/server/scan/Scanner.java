package app.cbo.oidc.java.server.scan;

import app.cbo.oidc.java.server.Server;
import app.cbo.oidc.java.server.scan.exceptions.*;
import app.cbo.oidc.java.server.utils.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class Scanner {


    private final static Logger LOGGER = Logger.getLogger(Scanner.class.getCanonicalName());

    private final String profile;
    private final Properties properties = new Properties();

    private final Set<Class<?>> classes;

    private final Map<ClassId, BuildStatus> buildProgress = new HashMap<>();
    private final Map<ClassId, Object> instances = new HashMap<>();


    public Scanner(String profile, String basePackage) throws IOException {
        this.profile = profile;
        this.classes = scanPackage(basePackage);
    }

    public Scanner(String basePackage) throws IOException {
        this(Injectable.DEFAULT, basePackage);
    }

    /**
     * TRASH
     **/
    public static void main(String... args) throws IOException, DownStreamException {
        {
            var scanner = new Scanner("app.cbo.oidc.java.server");
            scanner.loadProperties(Pair.of("basePath", "c:\\work"));
            var server = scanner.get(Server.class);
            server.start();
        }
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

    /**
     * TRASH
     **/


    public void loadProperties(Pair<String, String>... properties) {
        Stream.of(properties).forEachOrdered(pair -> this.properties.add(pair.left(), pair.right()));
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
            if (propertyKey.isPresent()) {

                args.add(this.properties.get(propertyKey.get(), constrArg));
            } else {
                try {
                    args.add(this.get(constrArg));
                } catch (InvalidDepTree e) {
                    throw new DownStreamException(t, e);
                } catch (DownStreamException e) {
                    throw new DownStreamException(t, e);
                }
            }
        }

        T built = null;
        try {
            built = constructor.newInstance(args.toArray());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new InstanciationFails(t, e);
        }
        this.buildProgress.put(ClassId.of(implementation), BuildStatus.BUILT);
        this.instances.put(ClassId.of(implementation), built);
        return built;
    }

    private Optional<String> getPropertyKey(Annotation[] annotations) {

        return Stream.of(annotations)
                .filter(a -> a.annotationType() == Prop.class)
                .map(a -> (Prop) a)
                .map(Prop::value)
                .findFirst();

    }

    private <T> Class<T> findImplementation(Class<T> t) throws NoImplementationFound {

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
                    .filter(c -> c.getAnnotation(Injectable.class).value().equals(this.profile))
                    .toList());
            return (Class<T>) forThisProfile;
        } catch (NoSingleResult noResult) {
            throw new NoImplementationFound(t, noResult);
        }

    }

    public <T> Constructor<T> findConstructor(Class<T> c) throws NoConstructorFound {

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
