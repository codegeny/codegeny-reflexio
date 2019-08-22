package org.codegeny.reflexio;

import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static org.codegeny.reflexio.TypeAssertions.isVariable;
import static java.util.function.Predicate.isEqual;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ArgumentTypesResolverTest {

    interface Converter<From, To> {
    }

    interface ToStringConverter<X> extends Serializable, Converter<X, String> {
    }

    static abstract class AbstractToStringConverter<NotUsed, Z> implements ToStringConverter<Z> {
    }

    static class UUIDToStringConverter extends AbstractToStringConverter<Void, UUID> {
    }

    public <A, B, C extends AbstractToStringConverter<A, B>> void something(C converter, Consumer<? extends AbstractToStringConverter<A, B>> c) {
    }

    @Test
    public void test() throws Exception {
        assertTypeParameters(Converter.class, isVariable("From"), isVariable("To"));
        assertTypeParameters(ToStringConverter.class, isVariable("X"), isEqual(String.class));
        assertTypeParameters(AbstractToStringConverter.class, isVariable("Z"), isEqual(String.class));
        assertTypeParameters(UUIDToStringConverter.class, isEqual(UUID.class), isEqual(String.class));
        assertTypeParameters(ArgumentTypesResolverTest.class.getMethod("something", AbstractToStringConverter.class, Consumer.class).getGenericParameterTypes()[0], isVariable("B"), isEqual(String.class));
        assertTypeParameters(((ParameterizedType) ArgumentTypesResolverTest.class.getMethod("something", AbstractToStringConverter.class, Consumer.class).getGenericParameterTypes()[1]).getActualTypeArguments()[0], isVariable("B"), isEqual(String.class));
    }

    @SafeVarargs
    private static void assertTypeParameters(Type klass, Predicate<? super Type>... predicates) {
        ParameterizedType parameterizedType = Types.findParameterizedType(klass, Converter.class);
        assertNotNull(parameterizedType);
        assertEquals(parameterizedType.getRawType(), Converter.class);
        Type[] args = parameterizedType.getActualTypeArguments();
        assertEquals(predicates.length, args.length);
        IntStream.range(0, predicates.length).forEach(i -> assertTrue(predicates[i].test(args[i])));
    }
}
