package org.codegeny.reflexio;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Map;

public class TypeVariableReplacer implements TypeVisitor<Type> {

    private final Map<TypeVariable<?>, Type> replaces;

    public TypeVariableReplacer(Map<TypeVariable<?>, Type> replaces) {
        this.replaces = replaces;
    }

    @Override
    public <T> Type visitClass(Class<T> klass) {
        return klass;
    }

    @Override
    public Type visitParameterizedType(ParameterizedType parameterizedType) {
        return Types.newParameterizedType(
                TypeVisitor.accept(this, parameterizedType.getRawType()),
                TypeVisitor.accept(this, parameterizedType.getOwnerType()),
                TypeVisitor.accept(this, Type[]::new, parameterizedType.getActualTypeArguments())
        );
    }

    @Override
    public Type visitGenericArrayType(GenericArrayType genericArrayType) {
        return Types.newGenericArrayType(
                TypeVisitor.accept(this, genericArrayType.getGenericComponentType())
        );
    }

    @Override
    public Type visitWildcardType(WildcardType wildcardType) {
        return Types.newWildcardType(
                TypeVisitor.accept(this, Type[]::new, wildcardType.getLowerBounds()),
                TypeVisitor.accept(this, Type[]::new, wildcardType.getUpperBounds())
        );
    }

    @Override
    public <D extends GenericDeclaration> Type visitTypeVariable(TypeVariable<D> typeVariable) {
        return replaces.getOrDefault(typeVariable, typeVariable);
    }
}
