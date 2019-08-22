package org.codegeny.reflexio;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

enum MemberTypeExtractor implements MemberVisitor<Type> {

    INSTANCE;

    @Override
    public Type visitField(Field field) {
        return field.getGenericType();
    }

    @Override
    public <T> Type visitConstructor(Constructor<T> constructor) {
        return constructor.getDeclaringClass();
    }

    @Override
    public Type visitMethod(Method method) {
        return method.getGenericReturnType();
    }
}
