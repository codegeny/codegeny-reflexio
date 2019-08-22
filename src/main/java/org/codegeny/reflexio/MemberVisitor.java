package org.codegeny.reflexio;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.function.IntFunction;
import java.util.stream.Stream;

public interface MemberVisitor<R> {

    default R visitField(Field field) {
        throw new UnsupportedOperationException("Not defined for Field");
    }

    default <T> R visitConstructor(Constructor<T> constructor) {
        throw new UnsupportedOperationException("Not defined for Constructor");
    }

    default R visitMethod(Method method) {
        throw new UnsupportedOperationException("Not defined for Method");
    }

    static <R> R[] accept(MemberVisitor<R> visitor, IntFunction<R[]> generator, Member... members) {
        return Stream.of(members).map(member -> accept(visitor, member)).toArray(generator);
    }

    static <R> R accept(MemberVisitor<R> visitor, Member member) {
        if (member == null) {
            return null;
        }
        if (member instanceof Field) {
            return visitor.visitField((Field) member);
        }
        if (member instanceof Constructor<?>) {
            return visitor.visitConstructor((Constructor<?>) member);
        }
        if (member instanceof Method) {
            return visitor.visitMethod((Method) member);
        }
        throw new IllegalArgumentException("Unknown member");
    }
}
