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
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Homegrown dependency injection engine. Do not do this in real life.
 */
public class Scanner {


    private final static Logger LOGGER = Logger.getLogger(Scanner.class.getCanonicalName());


    private final String profile;
    private final app.cbo.oidc.java.server.scan.props.Properties properties = new Properties();

    private final Set<Class<?>> classes;

    private final Map<ClassId<?>, BuildStatus> buildProgress = new HashMap<>();
    private final Map<ClassId<?>, Object> instances = new HashMap<>();


    /**
     * Will prepare dependencies, looking implementations in the provided package and its subpackage
     * @param profile any class annotated with @Injectable will be considered only if it has the same value as profile
     * @param basePackage where the implementations will be searched
     * @throws IOException classLoader issues
     */
    public Scanner(String profile, String basePackage) throws IOException {
        this(profile, basePackage, Scanner::scanPackage);
    }

    /**
     * Will prepare dependencies, looking implementations in the provided package and its subpackage
     * @param profile any class annotated with @Injectable will be considered only if it has the same value as profile
     * @param basePackage where the implementations will be searched
     * @param packageScanner a function that will scan the package and return the classes found
     */
    public Scanner(String profile, String basePackage, Function<String, Set<Class<?>>> packageScanner) {
        this.profile = profile;

        this.classes = packageScanner.apply(basePackage);//scanPackage(basePackage);
    }

    /**
     * Will prepare dependencies, looking implementations in the provided package and its subpackage. Porifle will be "default"
     * @param basePackage where the implementations will be searched
     * @throws IOException classLoader issues
     */
    public Scanner(String basePackage) throws IOException {
        this(Injectable.DEFAULT, basePackage);
    }

    /**
     * Allows to provide properties to the scanner, allowing setting of @Prop arguments
     * @param properties list of properties (key/value)
     * @return the scanner
     */
    public Scanner withProperties(List<Pair<String, String>> properties) {
        properties.forEach(pair -> this.properties.add(pair.left(), pair.right()));
        return this;
    }


    /**
     * Homegrown package scanner
     * @param packageName where the implementations will be searched
     * @return Set of classes found in the package and its subpackages
     */
    public static Set<Class<?>> scanPackage(String packageName)  {

        LOGGER.info("Scanning " + packageName);

        var sysTemClassLoader = ClassLoader.getSystemClassLoader();

        Set<Class<?>> classes = new HashSet<>();

        InputStream stream = sysTemClassLoader.getResourceAsStream(packageName.replaceAll("[.]", "/"));

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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return classes;
    }

    private static Optional<Class<?>> getClass(String className, String packageName) {
        try {
            return Optional.of(Class.forName(packageName + "." + className.substring(0, className.lastIndexOf('.'))));
        } catch (ClassNotFoundException e) {
            // handle the exception
            LOGGER.severe("Class not found : " + className);
            return Optional.empty();
        }
    }


    /**
     * Return the implementation for a class
     * @param dependency class to be instanciated
     * @return implementation
     * @param <T> type of the dependency
     * @throws DownStreamException when a dep of the dep cannot be built
     */
    public <T> T get(Class<T> dependency) throws DownStreamException {

        //first look for the Implementation class
        Class<T> implementation;
        if (!dependency.isInterface()) {
            //easy, it's a class
            implementation = dependency;
        } else {
            //we have to find the correct class for this interface
            implementation = this.findImplementation(dependency);
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
                throw new DownStreamException(dependency, e);
            } catch (DownStreamException e) {
                throw new DownStreamException(dependency, e);
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
            throw new InstanciationFails(dependency, e);
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

    <T> Class<T> findImplementation(Class<T> interfaceToImpl) throws NoImplementationFound {

        //all classes that are not the interface, not another interface, and implements the interface
        var implementations = this.classes
                .stream()
                .filter(c -> !c.equals(interfaceToImpl))
                .filter(c -> !c.isInterface())
                .filter(interfaceToImpl::isAssignableFrom)
                .filter(c -> c.isAnnotationPresent(Injectable.class))
                .toList();

        LOGGER.fine(implementations.size() + " implementations found for " + interfaceToImpl.getCanonicalName());

        //is there an impl for the profile ?
        try {
            final Class<?> forThisProfile = ScanUtils.oneAndOnlyOne(implementations
                    .stream()
                    .filter(c -> c.getAnnotation(Injectable.class).value().equals(this.profile))
                    .toList());
            return (Class<T>) forThisProfile;

        } catch (NoResult noResult) {
            //nothing, we have plan B
        } catch (TooManyResult tooManyResult) {
            throw new NoImplementationFound(interfaceToImpl, tooManyResult);
        }

        //is there a default impl ?
        try {
            final Class<?> forThisProfile = ScanUtils.oneAndOnlyOne(implementations
                    .stream()
                    .filter(c -> c.getAnnotation(Injectable.class).value().equals(Injectable.DEFAULT))
                    .toList());
            return (Class<T>) forThisProfile;
        } catch (NoSingleResult noResult) {
            throw new NoImplementationFound(interfaceToImpl, noResult);
        }

    }

    /**
     * Locate the correct constr. to be used to instanciate the implementation
     * @param implementation class to instanciate
     * @return instance
     * @param <T> type of the implementation
     * @throws NoConstructorFound when no constructor is found
     */
    <T> Constructor<T> findConstructor(Class<T> implementation) throws NoConstructorFound {

        //we only have one, use it
        if (implementation.getDeclaredConstructors().length == 1) {
            return (Constructor<T>) implementation.getDeclaredConstructors()[0];
        }

        var constructors = Stream.of(implementation.getDeclaredConstructors())
                .filter(constructor -> constructor.isAnnotationPresent(BuildWith.class))
                .toList();

        try {
            //find the constructor annotated with @BuildWith
            return (Constructor<T>) ScanUtils.oneAndOnlyOne(constructors);
        } catch (NoSingleResult noResult) {
            throw new NoConstructorFound(implementation);
        }
    }
}
