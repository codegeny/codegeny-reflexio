package org.codegeny.reflexio;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

enum RawClassResolver implements TypeVisitor<Class<?>> {

    INSTANCE;

    @Override
    public <T> Class<?> visitClass(Class<T> klass) {
        return klass;
    }

    @Override
    public Class<?> visitParameterizedType(ParameterizedType parameterizedType) {
        return TypeVisitor.accept(this, parameterizedType.getRawType());
    }

    @Override
    public Class<?> visitGenericArrayType(GenericArrayType genericArrayType) {
        return TypeVisitor.accept(this, Types.arrayType(TypeVisitor.accept(this, genericArrayType.getGenericComponentType())));
    }

    @Override
    public Class<?> visitWildcardType(WildcardType wildcardType) {
        return TypeVisitor.accept(this, wildcardType.getUpperBounds()[0]);
    }

    @Override
    public <D extends GenericDeclaration> Class<?> visitTypeVariable(TypeVariable<D> typeVariable) {
        return TypeVisitor.accept(this, typeVariable.getBounds()[0]);
    }
}
