package org.codegeny.reflexio;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class TypeVariableRemover implements TypeVisitor<Type> {

    private final Set<TypeVariable<?>> visited = new HashSet<>();

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
                Stream.of(wildcardType.getLowerBounds()).map(b -> TypeVisitor.accept(this, b)).filter(t -> !(t instanceof WildcardType)).toArray(Type[]::new),
                Stream.of(wildcardType.getUpperBounds()).map(b -> TypeVisitor.accept(this, b)).filter(t -> !(t instanceof WildcardType)).toArray(Type[]::new)
        );
        //TypeVisitor.accept(this, Type[]::new, wildcardType.getLowerBounds()),
        //TypeVisitor.accept(this, Type[]::new, wildcardType.getUpperBounds())
    }

    @Override
    public <D extends GenericDeclaration> Type visitTypeVariable(TypeVariable<D> typeVariable) {
        return visited.add(typeVariable)
                ? Types.newWildcardType(new Type[0], TypeVisitor.accept(this, Type[]::new, typeVariable.getBounds()))
                : Types.WILDCARD; // recursive :-(
    }
}
