package org.codegeny.reflexio;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.function.IntFunction;
import java.util.stream.Stream;

/**
 * Visitor for {@link Type}s.
 *
 * @param <R> The result type of the visitor.
 * @author Xavier DURY
 */
public interface TypeVisitor<R> {

    default <T> R visitClass(Class<T> klass) {
        throw new UnsupportedOperationException("Not defined for Class / " + getClass().getSimpleName());
    }

    default R visitParameterizedType(ParameterizedType parameterizedType) {
        throw new UnsupportedOperationException("Not defined for ParameterizedType / " + getClass().getSimpleName());
    }

    default R visitGenericArrayType(GenericArrayType genericArrayType) {
        throw new UnsupportedOperationException("Not defined for GenericArrayType / " + getClass().getSimpleName());
    }

    default R visitWildcardType(WildcardType wildcardType) {
        throw new UnsupportedOperationException("Not defined for WildcardType / " + getClass().getSimpleName());
    }

    default <D extends GenericDeclaration> R visitTypeVariable(TypeVariable<D> typeVariable) {
        throw new UnsupportedOperationException("Not defined for TypeVariable / " + getClass().getSimpleName());
    }

    static <R> R[] accept(TypeVisitor<R> visitor, IntFunction<R[]> generator, Type... types) {
        return Stream.of(types).map(type -> accept(visitor, type)).toArray(generator);
    }

    static <R> R accept(TypeVisitor<R> visitor, Type type) {
        if (type == null) {
            return null;
        }
        if (type instanceof Class<?>) {
            return visitor.visitClass((Class<?>) type);
        }
        if (type instanceof ParameterizedType) {
            return visitor.visitParameterizedType((ParameterizedType) type);
        }
        if (type instanceof GenericArrayType) {
            return visitor.visitGenericArrayType((GenericArrayType) type);
        }
        if (type instanceof WildcardType) {
            return visitor.visitWildcardType((WildcardType) type);
        }
        if (type instanceof TypeVariable<?>) {
            return visitor.visitTypeVariable((TypeVariable<?>) type);
        }
        throw new IllegalArgumentException("Unknown type");
    }
}
