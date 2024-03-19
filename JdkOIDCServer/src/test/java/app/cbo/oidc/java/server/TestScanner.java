package app.cbo.oidc.java.server;

import app.cbo.oidc.java.server.scan.PackageScanner;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.util.HashSet;
import java.util.Set;

public class TestScanner implements PackageScanner {

    public TestScanner() {
    }

    @Override
    public Set<Class<?>> apply(String s) {
        return this.findClassesInPackage(s);
    }

    public Set<Class<?>> findClassesInPackage(String packageName) {

        Reflections reflections = new Reflections(packageName, new SubTypesScanner(false));

        return new HashSet<>(reflections.getSubTypesOf(Object.class));


    }


}
