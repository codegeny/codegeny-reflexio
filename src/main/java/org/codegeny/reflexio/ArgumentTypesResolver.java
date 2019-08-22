package org.codegeny.reflexio;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;

final class ArgumentTypesResolver implements TypeVisitor<Type[]> {

    private final Class<?> reference;

    ArgumentTypesResolver(Class<?> reference) {
        this.reference = reference;
    }

    @Override
    public <T> Type[] visitClass(Class<T> klass) {
        if (reference.equals(klass)) {
            // May return Class[] instead of Type[], so copy it as a Type[] to avoid
            // problems in visit(ParameterizedType)
            return Arrays.copyOf(klass.getTypeParameters(), reference.getTypeParameters().length, Type[].class);
        }
        if (klass.getSuperclass() != null && reference.isAssignableFrom(klass.getSuperclass())) {
            return TypeVisitor.accept(this, klass.getGenericSuperclass());
        }
        Class<?>[] interfaces = klass.getInterfaces();
        Type[] genericInterfaces = klass.getGenericInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            if (reference.isAssignableFrom(interfaces[i])) {
                return TypeVisitor.accept(this, genericInterfaces[i]);
            }
        }
        throw new IllegalArgumentException(String.format("%s is not assignable to %s", klass, reference));
    }

    @Override
    public Type[] visitParameterizedType(ParameterizedType parameterizedType) {
        Class<?> rawType = (Class<?>) parameterizedType.getRawType(); // always a Class
        TypeVariable<?>[] typeVariables = rawType.getTypeParameters();
        Type[] types = TypeVisitor.accept(this, rawType);
        for (int i = 0; i < types.length; i++) {
            if (types[i] instanceof TypeVariable<?>) {
                TypeVariable<?> typeVariable = (TypeVariable<?>) types[i];
                for (int j = 0; j < typeVariables.length; j++) {
                    if (typeVariables[j].getName().equals(typeVariable.getName())) {
                        types[i] = parameterizedType.getActualTypeArguments()[j];
                    }
                }
            }
        }
        return types;
    }

    @Override
    public <D extends GenericDeclaration> Type[] visitTypeVariable(TypeVariable<D> typeVariable) {
        for (Type bound : typeVariable.getBounds()) {
            if (Types.isAssignable(reference, bound)) {
                return TypeVisitor.accept(this, bound);
            }
        }
        throw new IllegalArgumentException(String.format("%s is not assignable to %s", typeVariable, reference));
    }

    @Override
    public Type[] visitGenericArrayType(GenericArrayType genericArrayType) {
        throw new IllegalArgumentException(String.format("%s is not assignable to %s", genericArrayType, reference));
    }

    @Override
    public Type[] visitWildcardType(WildcardType wildcardType) {
        for (Type upperBound : wildcardType.getUpperBounds()) {
            if (Types.isAssignable(reference, upperBound)) {
                return TypeVisitor.accept(this, upperBound);
            }
        }
        throw new IllegalArgumentException(String.format("%s is not assignable to %s", wildcardType, reference));
    }
}
