package app.cbo.oidc.java.server.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class EnumValuesHelper {


    /**
     * Converts a String to the provided enum
     * @param paramValue string (found in query string)
     * @param values All possible values
     * @param <E> type of the enum
     * @return the enum value if matchig, empty if not
     */
    public static <E extends Enum<E> & ParamEnum> Optional<E> fromParam(String paramValue, E... values){

        return Stream.of(values)
                .filter(e -> e.paramValue().equals(paramValue))
                .findFirst();
    }

    /**
     * Converts some Strinsg to the provided enum
     * @param paramValues string (found in query string)
     * @param values All possible values
     * @param <E> type of the enum
     * @return the enum values
     */
    public static <E extends Enum<E> & ParamEnum> List<E> fromParams(Collection<String> paramValues, E... values){

        if(paramValues == null){
            return Collections.emptyList();
        }

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
