package org.codegeny.reflexio;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

final class TypeVariableExpander implements TypeVisitor<Type> {

    private final GenericDeclarationVisitor<Type[]> argsVisitor = new GenericDeclarationVisitor<Type[]>() {

        @Override
        public <T> Type[] visitClass(Class<T> klass) {
            return new ArgumentTypesResolver(klass).visitClass(reference);
        }
    };

    private final Class<?> reference;

    TypeVariableExpander(Class<?> reference) {
        this.reference = reference;
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
    public Type visitWildcardType(WildcardType wildcardType) {
        return Types.newWildcardType(
                TypeVisitor.accept(this, Type[]::new, wildcardType.getLowerBounds()),
                TypeVisitor.accept(this, Type[]::new, wildcardType.getUpperBounds())
        );
    }

    @Override
    public Type visitGenericArrayType(GenericArrayType genericArrayType) {
        return Types.newGenericArrayType(TypeVisitor.accept(this, genericArrayType.getGenericComponentType()));
    }

    @Override
    public <D extends GenericDeclaration> Type visitTypeVariable(TypeVariable<D> typeVariable) {
        Type[] args = GenericDeclarationVisitor.accept(argsVisitor, typeVariable.getGenericDeclaration());
        TypeVariable<?>[] variables = typeVariable.getGenericDeclaration().getTypeParameters();
        for (int i = 0; i < variables.length; i++) {
            if (variables[i].getName().equals(typeVariable.getName())) {
                return args[i].equals(typeVariable) ? args[i] : TypeVisitor.accept(this, args[i]);
            }
        }
        return typeVariable;
    }
}
