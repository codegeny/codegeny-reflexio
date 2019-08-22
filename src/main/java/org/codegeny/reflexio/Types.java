package org.codegeny.reflexio;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Member;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Types {

    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER;

    static {
        Map<Class<?>, Class<?>> primitiveToWrapper = new HashMap<>();
        primitiveToWrapper.put(boolean.class, Boolean.class);
        primitiveToWrapper.put(char.class, Character.class);
        primitiveToWrapper.put(byte.class, Byte.class);
        primitiveToWrapper.put(short.class, Short.class);
        primitiveToWrapper.put(int.class, Integer.class);
        primitiveToWrapper.put(long.class, Long.class);
        primitiveToWrapper.put(float.class, Float.class);
        primitiveToWrapper.put(double.class, Double.class);
        primitiveToWrapper.put(void.class, Void.class);
        PRIMITIVE_TO_WRAPPER = Collections.unmodifiableMap(primitiveToWrapper);
    }

    public static Class<?> primitiveToWrapper(Class<?> type) {
        return PRIMITIVE_TO_WRAPPER.getOrDefault(type, type);
    }

    /**
     * This method helps reconstructing the resulting ParameterizedType for a specific generic type across its hierarchy.
     * Example: Let's create some interface Converter[X, Y] and try to determine the actual type arguments for that
     * interface from classes/interfaces which inherit from it (directly or indirectly).
     * <ul>
     * <li>Converter[X, Y] will yield Converter[X, Y]</li>
     * <li>StringConverter[Z] extends Converter[Z, String] will yield Converter[Z, String]</li>
     * <li>AbstractStringConverter[A] implements StringConverter[A] will yield Converter[A, String]</li>
     * <li>UUIDStringConverter extends AbstractStringConverter[UUID] will yield Converter[UUID, String]</li>
     * </ul>
     * For the last example (UUIDStringConverter), nowhere in its hierarchy is a type directly implementing
     * Converter[UUID, String] but this method is capable of reconstructing that information.
     *
     * @param type      The current class (for example: StringConverter)
     * @param reference The reference class (for example: Converter)
     * @return A parameterized type.
     */
    public static ParameterizedType findParameterizedType(Type type, Class<?> reference) {
        return newParameterizedType(reference, null, resolveTypeArguments(type, reference));
    }

    public static Type[] resolveTypeArguments(Type type, Class<?> reference) {
        return TypeVisitor.accept(new ArgumentTypesResolver(reference), type);
    }

    public static <A extends Annotation> Set<A> collect(AnnotatedElement element, Class<A> annotationType) {
        return AnnotatedElementVisitor.accept(new CollectAnnotationsElementVisitor<>(annotationType), element);
    }

    public static boolean isAssignable(Type left, Type right) {
        return Boolean.TRUE.equals(TypeVisitor.accept(TypeVisitor.accept(AssignabilityTypeVisitor.INSTANCE, left), right));
    }

    public static Class<?> raw(Type type) {
        return TypeVisitor.accept(RawClassResolver.INSTANCE, type);
    }

    public static Type expand(Type type, Class<?> reference) {
        return TypeVisitor.accept(new TypeVariableExpander(reference), type);
    }

    public static Type expand(Member member, Class<?> reference) {
        return expand(MemberVisitor.accept(MemberTypeExtractor.INSTANCE, member), reference);
    }

    public static ParameterizedType newParameterizedType(Type rawType, Type ownerType, Type... arguments) {
        return new ParameterizedTypeImpl(rawType, ownerType, arguments);
    }

    public static TypeVariable<?> classTypeVariable(String name, Class<?> klass) {
        return findTypeVariable(name, klass);
    }

    public static TypeVariable<?> methodTypeVariable(String name, Class<?> klass, String method, Class<?>... args) {
        try {
            return findTypeVariable(name, klass.getMethod(method, args));
        } catch (NoSuchMethodException e) {
            throw new NoSuchElementException(e.getMessage());
        }
    }

    public static TypeVariable<?> constructorTypeVariable(String name, Class<?> klass, Class<?>... args) {
        try {
            return findTypeVariable(name, klass.getConstructor(args));
        } catch (NoSuchMethodException e) {
            throw new NoSuchElementException(e.getMessage());
        }
    }

    private static TypeVariable<?> findTypeVariable(String name, GenericDeclaration declaration) {
        return Stream.of(declaration.getTypeParameters())
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Cant find TypeVariable named '" + name + "' on " + declaration));
    }

    public static GenericArrayType newGenericArrayType(Type component) {
        return new GenericArrayTypeImpl(component);
    }

    public static Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, Thread.currentThread().getContextClassLoader());
    }

    public static Class<?> loadClass(String name, ClassLoader classLoader) throws ClassNotFoundException {
        switch (name) {
            case "boolean":
                return boolean.class;
            case "char":
                return char.class;
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "void":
                return void.class;
            default:
                return classLoader.loadClass(name);
        }
    }

    public static Type arrayType(Type component, int dimension) {
        if (dimension < 0) {
            throw new IllegalArgumentException("Array dimension must be positive");
        }
        if (dimension == 0) {
            return component;
        }
        if (component instanceof Class<?>) {
            Class<?> klass = (Class<?>) component;
            StringBuilder builder = new StringBuilder();
            while (dimension-- > 0) {
                builder.append('[');
            }
            if (klass.isArray()) {
                builder.append(klass.getName());
            } else if (klass.isPrimitive()) {
                builder.append(klass == long.class ? 'J' : Character.toUpperCase(klass.getName().charAt(0)));
            } else {
                builder.append('L').append(klass.getName()).append(';');
            }
            try {
                return Class.forName(builder.toString(), false, klass.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new InternalError("Loading an array class from an already loaded component class should not fail", e);
            }
        }
        return arrayType(newGenericArrayType(component), dimension - 1);
    }

    public static WildcardType newWildcardType(Type[] lowerBounds, Type[] upperBounds) {
        return new WildcardTypeImpl(lowerBounds, upperBounds);
    }

    private static class WildcardTypeImpl implements WildcardType {

        private final Type[] lowerBounds;
        private final Type[] upperBounds;

        WildcardTypeImpl(Type[] lowerBounds, Type[] upperBounds) {
            this.lowerBounds = Objects.requireNonNull(lowerBounds);
            this.upperBounds = Objects.requireNonNull(upperBounds);
        }

        @Override
        public Type[] getUpperBounds() {
            return upperBounds;
        }

        @Override
        public Type[] getLowerBounds() {
            return lowerBounds;
        }

        @Override
        public boolean equals(Object that) {
            return super.equals(that) || that instanceof WildcardType && equals((WildcardType) that);
        }

        private boolean equals(WildcardType that) {
            return Arrays.equals(lowerBounds, that.getLowerBounds()) && Arrays.equals(upperBounds, that.getUpperBounds());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(lowerBounds) ^ Arrays.hashCode(upperBounds);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("?");
            if (lowerBounds.length > 0) {
                builder.append(" super ").append(Stream.of(lowerBounds).map(Type::getTypeName).collect(Collectors.joining(", ")));
            }
            if (upperBounds.length > 0) {
                builder.append(" extends ").append(Stream.of(upperBounds).map(Type::getTypeName).collect(Collectors.joining(", ")));
            }
            return builder.toString();
        }
    }

    private static class ParameterizedTypeImpl implements ParameterizedType {

        private final Type rawType;
        private final Type ownerType;
        private final Type[] arguments;

        ParameterizedTypeImpl(Type rawType, Type ownerType, Type... arguments) {
            this.rawType = Objects.requireNonNull(rawType);
            this.ownerType = ownerType;
            this.arguments = Objects.requireNonNull(arguments);
        }

        @Override
        public Type getRawType() {
            return rawType;
        }

        @Override
        public Type getOwnerType() {
            return ownerType;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return arguments;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(rawType)
                    ^ Objects.hashCode(ownerType)
                    ^ Arrays.hashCode(arguments);
        }

        @Override
        public boolean equals(Object that) {
            return super.equals(that) || that instanceof ParameterizedType && equals((ParameterizedType) that);
        }

        private boolean equals(ParameterizedType that) {
            return Objects.equals(rawType, that.getRawType())
                    && Objects.equals(ownerType, that.getOwnerType())
                    && Arrays.equals(arguments, that.getActualTypeArguments());
        }

        @Override
        public String toString() {
            return rawType.getTypeName().concat(Stream.of(getActualTypeArguments()).map(Type::getTypeName).collect(Collectors.joining(",", "<", ">")));
        }
    }

    private static class GenericArrayTypeImpl implements GenericArrayType {

        private final Type component;

        GenericArrayTypeImpl(Type component) {
            this.component = Objects.requireNonNull(component);
        }

        @Override
        public Type getGenericComponentType() {
            return component;
        }

        @Override
        public boolean equals(Object that) {
            return super.equals(that) || that instanceof GenericArrayType && equals((GenericArrayType) that);
        }

        private boolean equals(GenericArrayType that) {
            return Objects.equals(component, that.getGenericComponentType());
        }

        @Override
        public int hashCode() {
            return Objects.hash(component);
        }

        @Override
        public String toString() {
            return component.getTypeName().concat("[]");
        }
    }

    private Types() {
        throw new InternalError();
    }
}
