package org.codegeny.reflexio;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.function.IntFunction;
import java.util.stream.Stream;

public interface AnnotatedElementVisitor<R> {

    default <T> R visit(Class<T> klass) {
        throw new UnsupportedOperationException();
    }

    default <T> R visit(Constructor<T> constructor) {
        throw new UnsupportedOperationException();
    }

    default R visit(Method method) {
        throw new UnsupportedOperationException();
    }

    default R visit(Field field) {
        throw new UnsupportedOperationException();
    }

    default R visit(Parameter parameter) {
        throw new UnsupportedOperationException();
    }

    default R visit(Package pakkage) {
        throw new UnsupportedOperationException();
    }

    static <R> R[] accept(AnnotatedElementVisitor<R> visitor, IntFunction<R[]> generator, AnnotatedElement... elements) {
        return Stream.of(elements).map(element -> accept(visitor, element)).toArray(generator);
    }

    static <R> R accept(AnnotatedElementVisitor<R> visitor, AnnotatedElement element) {
        if (element == null) {
            return null;
        }
        if (element instanceof Class<?>) {
            return visitor.visit((Class<?>) element);
        }
        if (element instanceof Constructor<?>) {
            return visitor.visit((Constructor<?>) element);
        }
        if (element instanceof Method) {
            return visitor.visit((Method) element);
        }
        if (element instanceof Field) {
            return visitor.visit((Field) element);
        }
        if (element instanceof Parameter) {
            return visitor.visit((Parameter) element);
        }
        if (element instanceof Package) {
            return visitor.visit((Package) element);
        }
        throw new IllegalArgumentException("not supported");
    }
}
