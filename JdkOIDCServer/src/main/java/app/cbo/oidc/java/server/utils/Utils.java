package app.cbo.oidc.java.server.utils;

import java.util.Collection;
import java.util.Optional;

public class Utils {

    public static boolean isEmpty(String s){
        return s == null || s.isEmpty();
    }
    public static boolean isBlank(String s){
        return isEmpty(s) || s.isBlank();
    }

    public static boolean isEmpty(Optional<String> s){

        return s == null || s.isEmpty() || s.get().isEmpty();
    }
    public static boolean isBlank(Optional<String> s){
        return isEmpty(s) || s.get().isBlank();
    }

    public static boolean isEmpty(Collection<?> it){
        return (it == null || it.isEmpty());
    }
    public static boolean isBlank(Collection<String> strings){
        return isEmpty(strings) || strings.stream().allMatch(Utils::isBlank);
    }
}
