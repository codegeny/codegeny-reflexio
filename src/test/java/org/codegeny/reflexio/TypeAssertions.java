package org.codegeny.reflexio;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.function.Predicate;

public final class TypeAssertions {

    public static <S, T extends S> Predicate<S> isInstanceOf(Class<T> type, Predicate<? super T> predicate) {
        return o -> type.isInstance(o) && predicate.test(type.cast(o));
    }

    public static Predicate<Type> isVariable(String name) {
        return isInstanceOf(TypeVariable.class, tv -> tv.getName().equals(name));
    }

    public static boolean isTypeVariable(Type type, String name) {
        return type instanceof TypeVariable<?> && ((TypeVariable<?>) type).getName().equals(name);
    }

    private TypeAssertions() {
        throw new InternalError();
    }
}
