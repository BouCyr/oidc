package app.cbo.oidc.java.server.utils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class EnumValuesHelper {



    public static <E extends Enum<E> & ParamEnum> Optional<E> fromParam(String paramValue, E... values){

        return Stream.of(values)
                .filter(e -> e.paramValue().equals(paramValue))
                .findFirst();
    }

    public static <E extends Enum<E> & ParamEnum> List<E> fromParams(Collection<String> paramValues, E... values){

        return paramValues.stream()
                .map(s -> fromParam(s, values))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }


    public interface ParamEnum{
        String paramValue();
    }
}
