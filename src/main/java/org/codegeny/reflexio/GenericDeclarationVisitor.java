package org.codegeny.reflexio;

import java.lang.reflect.Constructor;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.util.function.IntFunction;
import java.util.stream.Stream;

public interface GenericDeclarationVisitor<R> {

    default <T> R visitClass(Class<T> klass) {
        throw new UnsupportedOperationException("Not defined for Class");
    }

    default <T> R visitConstructor(Constructor<T> constructor) {
        throw new UnsupportedOperationException("Not defined for Constructor " + constructor);
    }

    default R visitMethod(Method method) {
        throw new UnsupportedOperationException("Not defined for Method " + method);
    }

    static <R> R[] accept(GenericDeclarationVisitor<R> visitor, IntFunction<R[]> generator, GenericDeclaration... declarations) {
        return Stream.of(declarations).map(declaration -> accept(visitor, declaration)).toArray(generator);
    }

    static <R> R accept(GenericDeclarationVisitor<R> visitor, GenericDeclaration declaration) {
        if (declaration == null) {
            return null;
        }
        if (declaration instanceof Class<?>) {
            return visitor.visitClass((Class<?>) declaration);
        }
        if (declaration instanceof Constructor<?>) {
            return visitor.visitConstructor((Constructor<?>) declaration);
        }
        if (declaration instanceof Method) {
            return visitor.visitMethod((Method) declaration);
        }
        throw new IllegalArgumentException("Unknown generic declaration");
    }
}
