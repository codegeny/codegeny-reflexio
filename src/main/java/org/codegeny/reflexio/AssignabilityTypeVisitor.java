package org.codegeny.reflexio;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class AssignabilityTypeVisitor implements TypeVisitor<Boolean> {

    private final Type right;
    private final Map<TypeVariable<?>, Type> captures;

    AssignabilityTypeVisitor(Type right, Map<TypeVariable<?>, Type> captures) {
        this.right = right;
        this.captures = captures;
    }

    @Override
    public <T> Boolean visitClass(Class<T> left) {
        return TypeVisitor.accept(new ClassAssignabilityTypeVisitor<>(left), right);
    }

    @Override
    public Boolean visitParameterizedType(ParameterizedType left) {
        return TypeVisitor.accept(new ParameterizedTypeAssignabilityTypeVisitor(left), right);
    }

    @Override
    public Boolean visitGenericArrayType(GenericArrayType left) {
        return TypeVisitor.accept(new GenericArrayTypeAssignabilityTypeVisitor(left), right);
    }

    @Override
    public Boolean visitWildcardType(WildcardType left) {
        return TypeVisitor.accept(new WildcardTypeAssignabilityTypeVisitor(left), right);
    }

    @Override
    public <D extends GenericDeclaration> Boolean visitTypeVariable(TypeVariable<D> left) {
        return TypeVisitor.accept(new TypeVariableAssignabilityTypeVisitor<>(left), right);
    }

    private Boolean isAssignable(Type left, Type right) {
        return Types.isAssignable(left, right, captures);
    }

    private class ClassAssignabilityTypeVisitor<K> implements TypeVisitor<Boolean> {

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
            Type captured = captures.get(right);
            if (captured != null) {
                return left.equals(captured);
            }
            return Stream.of(right.getBounds()).allMatch(t -> TypeVisitor.accept(this, t));
        }

        @Override
        public Boolean visitGenericArrayType(GenericArrayType right) {
            return left.isArray() && isAssignable(left.getComponentType(), right.getGenericComponentType());
        }

        @Override
        public Boolean visitWildcardType(WildcardType right) {
            return Stream.of(right.getLowerBounds()).allMatch(b -> isAssignable(b, left))
                    && Stream.of(right.getUpperBounds()).allMatch(b -> isAssignable(left, b));
        }
    }

    private class ParameterizedTypeAssignabilityTypeVisitor implements TypeVisitor<Boolean> {

        private final ParameterizedType left;

        ParameterizedTypeAssignabilityTypeVisitor(ParameterizedType left) {
            this.left = left;
        }

        @Override
        public <T> Boolean visitClass(Class<T> right) {
            try {
                return isAssignable(left, Types.findParameterizedType(right, (Class<?>) left.getRawType()));
            } catch (IllegalArgumentException iae) {
                // TODO IMPROVE THIS
                return false;
            }
        }

        @Override
        public Boolean visitParameterizedType(ParameterizedType right) {

            if (!isAssignable(left.getRawType(), right.getRawType())) {
                return false;
            }

            Type[] rightArguments = TypeVisitor.accept(new ArgumentTypesResolver((Class<?>) left.getRawType()), right);
            Type[] leftArguments = left.getActualTypeArguments();

            if (rightArguments.length != leftArguments.length) {
                throw new InternalError();
            }

            return IntStream.range(0, rightArguments.length).allMatch(i -> isAssignable(leftArguments[i], rightArguments[i]));
        }

        @Override
        public <D extends GenericDeclaration> Boolean visitTypeVariable(TypeVariable<D> right) {
            return Stream.of(right.getBounds()).allMatch(b -> isAssignable(left, b));
        }

        @Override
        public Boolean visitGenericArrayType(GenericArrayType right) {
            return false;
        }

        @Override
        public Boolean visitWildcardType(WildcardType right) {
            return Stream.of(right.getLowerBounds()).allMatch(b -> isAssignable(b, left))
                    && Stream.of(right.getUpperBounds()).allMatch(b -> isAssignable(left, b));
        }
    }

    private class GenericArrayTypeAssignabilityTypeVisitor implements TypeVisitor<Boolean> {

        private final GenericArrayType left;

        GenericArrayTypeAssignabilityTypeVisitor(GenericArrayType left) {
            this.left = left;
        }

        @Override
        public <T> Boolean visitClass(Class<T> klass) {
            return klass.isArray() && isAssignable(left.getGenericComponentType(), klass.getComponentType());
        }

        @Override
        public Boolean visitGenericArrayType(GenericArrayType right) {
            return isAssignable(left.getGenericComponentType(), right.getGenericComponentType());
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

    private class WildcardTypeAssignabilityTypeVisitor implements TypeVisitor<Boolean> {

        private final WildcardType left;

        WildcardTypeAssignabilityTypeVisitor(WildcardType left) {
            this.left = left;
        }

        @Override
        public <T> Boolean visitClass(Class<T> right) {
            return Stream.of(left.getLowerBounds()).allMatch(b -> isAssignable(right, b))
                    && Stream.of(left.getUpperBounds()).allMatch(b -> isAssignable(b, right));
        }

        @Override
        public Boolean visitParameterizedType(ParameterizedType right) {
            return false;
        }

        @Override
        public Boolean visitWildcardType(WildcardType right) {
            return Stream.of(left.getLowerBounds()).allMatch(b -> Stream.of(right.getLowerBounds()).allMatch(c -> isAssignable(c, b)))
                    && Stream.of(left.getUpperBounds()).allMatch(b -> Stream.of(right.getUpperBounds()).allMatch(c -> isAssignable(b, c)));
        }

        @Override
        public <D extends GenericDeclaration> Boolean visitTypeVariable(TypeVariable<D> right) {
            return Stream.of(left.getLowerBounds()).allMatch(b -> Stream.of(right.getBounds()).allMatch(c -> isAssignable(b, c)))
                    && Stream.of(left.getUpperBounds()).allMatch(b -> Stream.of(right.getBounds()).allMatch(c -> isAssignable(c, b)));
        }
    }

    private class TypeVariableAssignabilityTypeVisitor<K extends GenericDeclaration> implements TypeVisitor<Boolean> {

        private final TypeVariable<K> left;

        TypeVariableAssignabilityTypeVisitor(TypeVariable<K> left) {
            this.left = left;
        }

        @Override
        public <T> Boolean visitClass(Class<T> right) {
            Type captured = captures.get(left);
            if (captured != null) {
                return right.equals(captured);
            }
            captures.put(left, right);
            return Stream.of(left.getBounds()).allMatch(b -> isAssignable(b, right));
        }

        @Override
        public Boolean visitGenericArrayType(GenericArrayType right) {
            return false;
        }

        @Override
        public Boolean visitWildcardType(WildcardType right) {
            return false;
        }

        @Override
        public Boolean visitParameterizedType(ParameterizedType right) {
            return false;
        }

        @Override
        public <D extends GenericDeclaration> Boolean visitTypeVariable(TypeVariable<D> right) {
            return left.getName().equals(right.getName())
                    && left.getGenericDeclaration().equals(right.getGenericDeclaration())
                    || Stream.of(right.getBounds()).allMatch(t -> TypeVisitor.accept(this, t));
        }
    }
}
