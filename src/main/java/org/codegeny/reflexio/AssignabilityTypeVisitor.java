package org.codegeny.reflexio;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.stream.IntStream;
import java.util.stream.Stream;

enum AssignabilityTypeVisitor implements TypeVisitor<TypeVisitor<Boolean>> {

    INSTANCE;

    private static class ClassAssignabilityTypeVisitor<K> implements TypeVisitor<Boolean> {

        private final Class<K> left;

        ClassAssignabilityTypeVisitor(Class<K> left) {
            this.left = left;
        }

        @Override
        public <T> Boolean visitClass(Class<T> right) {
            return left.isAssignableFrom(right);
        }

        @Override
        public Boolean visitParameterizedType(ParameterizedType right) {
            return TypeVisitor.accept(this, right.getRawType());
        }

        @Override
        public <D extends GenericDeclaration> Boolean visitTypeVariable(TypeVariable<D> right) {
            return Stream.of(right.getBounds()).allMatch(t -> TypeVisitor.accept(this, t));
        }

        @Override
        public Boolean visitGenericArrayType(GenericArrayType right) {
            return left.isArray() && Types.isAssignable(left.getComponentType(), right.getGenericComponentType());
        }

        @Override
        public Boolean visitWildcardType(WildcardType right) {
            return Stream.of(right.getLowerBounds()).allMatch(b -> Types.isAssignable(b, left))
                    && Stream.of(right.getUpperBounds()).allMatch(b -> Types.isAssignable(left, b));
        }
    }

    private static class ParameterizedTypeAssignabilityTypeVisitor implements TypeVisitor<Boolean> {

        private final ParameterizedType left;

        ParameterizedTypeAssignabilityTypeVisitor(ParameterizedType left) {
            this.left = left;
        }

        @Override
        public <T> Boolean visitClass(Class<T> right) {
            return Types.isAssignable(left.getRawType(), right);
        }

        @Override
        public Boolean visitParameterizedType(ParameterizedType right) {

            if (!Types.isAssignable(left.getRawType(), right.getRawType())) {
                return false;
            }

            Type[] rightArguments = TypeVisitor.accept(new ArgumentTypesResolver((Class<?>) left.getRawType()), right);
            Type[] leftArguments = left.getActualTypeArguments();

            if (rightArguments.length != leftArguments.length) {
                throw new InternalError();
            }

            return IntStream.range(0, rightArguments.length).allMatch(i -> Types.isAssignable(leftArguments[i], rightArguments[i]));
        }

        @Override
        public <D extends GenericDeclaration> Boolean visitTypeVariable(TypeVariable<D> right) {
            return Stream.of(right.getBounds()).allMatch(b -> Types.isAssignable(left, b));
        }

        @Override
        public Boolean visitGenericArrayType(GenericArrayType right) {
            return false;
        }

        @Override
        public Boolean visitWildcardType(WildcardType right) {
            return Stream.of(right.getLowerBounds()).allMatch(b -> Types.isAssignable(b, left))
                    && Stream.of(right.getUpperBounds()).allMatch(b -> Types.isAssignable(left, b));
        }
    }

    private static class GenericArrayTypeAssignabilityTypeVisitor implements TypeVisitor<Boolean> {

        private final GenericArrayType left;

        GenericArrayTypeAssignabilityTypeVisitor(GenericArrayType left) {
            this.left = left;
        }

        @Override
        public <T> Boolean visitClass(Class<T> klass) {
            return klass.isArray() && Types.isAssignable(left.getGenericComponentType(), klass.getComponentType());
        }

        @Override
        public Boolean visitGenericArrayType(GenericArrayType right) {
            return Types.isAssignable(left.getGenericComponentType(), right.getGenericComponentType());
        }

        @Override
        public Boolean visitParameterizedType(ParameterizedType parameterizedType) {
            return false;
        }

        @Override
        public Boolean visitWildcardType(WildcardType wildcardType) {
            return false;
        }

        @Override
        public <D extends GenericDeclaration> Boolean visitTypeVariable(TypeVariable<D> typeVariable) {
            return false;
        }
    }

    private static class WildcardTypeAssignabilityTypeVisitor implements TypeVisitor<Boolean> {

        private final WildcardType left;

        WildcardTypeAssignabilityTypeVisitor(WildcardType left) {
            this.left = left;
        }

        @Override
        public <T> Boolean visitClass(Class<T> right) {
            return Stream.of(left.getLowerBounds()).allMatch(b -> Types.isAssignable(right, b))
                    && Stream.of(left.getUpperBounds()).allMatch(b -> Types.isAssignable(b, right));
        }

        @Override
        public Boolean visitParameterizedType(ParameterizedType right) {
            return false;
        }

        @Override
        public Boolean visitWildcardType(WildcardType right) {
            return Stream.of(left.getLowerBounds()).allMatch(b -> Stream.of(right.getLowerBounds()).allMatch(c -> Types.isAssignable(c, b)))
                    && Stream.of(left.getUpperBounds()).allMatch(b -> Stream.of(right.getUpperBounds()).allMatch(c -> Types.isAssignable(b, c)));
        }

        @Override
        public <D extends GenericDeclaration> Boolean visitTypeVariable(TypeVariable<D> right) {
            return Stream.of(left.getLowerBounds()).allMatch(b -> Stream.of(right.getBounds()).allMatch(c -> Types.isAssignable(b, c)))
                    && Stream.of(left.getUpperBounds()).allMatch(b -> Stream.of(right.getBounds()).allMatch(c -> Types.isAssignable(c, b)));
        }
    }

    private static class TypeVariableAssignabilityTypeVisitor<K extends GenericDeclaration> implements TypeVisitor<Boolean> {

        private final TypeVariable<K> left;

        TypeVariableAssignabilityTypeVisitor(TypeVariable<K> left) {
            this.left = left;
        }

        @Override
        public <T> Boolean visitClass(Class<T> right) {
            return false;
        }

        @Override
        public Boolean visitGenericArrayType(GenericArrayType genericArrayType) {
            return false;
        }

        @Override
        public Boolean visitWildcardType(WildcardType wildcardType) {
            return false;
        }

        @Override
        public Boolean visitParameterizedType(ParameterizedType parameterizedType) {
            return false;
        }

        @Override
        public <D extends GenericDeclaration> Boolean visitTypeVariable(TypeVariable<D> right) {
            return left.getName().equals(right.getName())
                    && left.getGenericDeclaration().equals(right.getGenericDeclaration())
                    || Stream.of(right.getBounds()).allMatch(t -> TypeVisitor.accept(this, t));
        }
    }

    @Override
    public <T> TypeVisitor<Boolean> visitClass(Class<T> left) {
        return new ClassAssignabilityTypeVisitor<>(left);
    }

    @Override
    public TypeVisitor<Boolean> visitParameterizedType(ParameterizedType left) {
        return new ParameterizedTypeAssignabilityTypeVisitor(left);
    }

    @Override
    public TypeVisitor<Boolean> visitGenericArrayType(GenericArrayType left) {
        return new GenericArrayTypeAssignabilityTypeVisitor(left);
    }

    @Override
    public TypeVisitor<Boolean> visitWildcardType(WildcardType left) {
        return new WildcardTypeAssignabilityTypeVisitor(left);
    }

    @Override
    public <D extends GenericDeclaration> TypeVisitor<Boolean> visitTypeVariable(TypeVariable<D> left) {
        return new TypeVariableAssignabilityTypeVisitor<>(left);
    }
}
