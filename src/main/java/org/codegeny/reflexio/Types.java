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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Collection of static utility methods for java Types.
 *
 * @author Xavier DURY
 */
public final class Types {

    /**
     * Map primitive -> wrapper.
     */
    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER;

    /**
     * Convert a primitive to its wrapper if needed.
     *
     * @param type The type to wrap.
     * @return The wrapped type (or the original type if it was not primitive).
     */
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

    public static ParameterizedType asParameterizedType(Class<?> reference) {
        return findParameterizedType(reference, reference);
    }

    public static Type[] resolveTypeArguments(Type type, Class<?> reference) {
        return TypeVisitor.accept(new ArgumentTypesResolver(reference), type);
    }

    public static <A extends Annotation> Set<A> collect(AnnotatedElement element, Class<A> annotationType) {
        return AnnotatedElementVisitor.accept(new CollectAnnotationsElementVisitor<>(annotationType), element);
    }

    /**
     * Check if the right type is assignable to the left type (left := right). If any of the types contains
     * TypeVariables, this method will always return false.
     *
     * @param left  The left type.
     * @param right The right type.
     * @return True if the right is assignable to left.
     */
    public static boolean isAssignable(Type left, Type right) {
        Map<TypeVariable<?>, Type> captures = new HashMap<>();
        return isAssignable(left, right, captures) && captures.isEmpty();
    }

    /**
     * Check if the right type is assignable to the left type (left := right).
     * If Set[E] is checked against Set[String] and the map does not contains [E], this method will
     * return true with ([E] => String) added to the map.
     * If Set[E] is checked against Set[String] and the map already contains a key [E], this method will
     * return true only if the value for the key [E] is assignable to String.
     *
     * @param left     The left type.
     * @param right    The right type.
     * @param captures A map which already contains captures or into which new captures will be added.
     * @return True if the right is assignable to left.
     */
    public static boolean isAssignable(Type left, Type right, Map<TypeVariable<?>, Type> captures) {
        return Boolean.TRUE.equals(TypeVisitor.accept(new AssignabilityTypeVisitor(right, captures), left));
    }

    /**
     * Determine the raw Class from a Type.
     *
     * @param type The type.
     * @return The class.
     */
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

    /**
     * Convert the given component type to an array type.
     * A rank 0 returns the component as is, a rank 1 returns a component[], a rank 2 returns a component[][]...
     *
     * @param component The component type.
     * @return An array type of the given component type.
     */
    public static Type arrayType(Type component) {
        return arrayType(component, 1);
    }

    /**
     * Convert the given component type to an array type.
     * A rank 0 returns the component as is, a rank 1 returns a component[], a rank 2 returns a component[][]...
     *
     * @param component The component type.
     * @param rank      The rank.
     * @return An array type of the given component type.
     */
    public static Type arrayType(Type component, int rank) {
        if (rank < 0) {
            throw new IllegalArgumentException("Rank must be positive");
        }
        if (rank == 0) {
            return component;
        }
        if (component instanceof Class<?>) {
            Class<?> klass = (Class<?>) component;
            StringBuilder builder = new StringBuilder();
            while (rank-- > 0) {
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
        return arrayType(newGenericArrayType(component), rank - 1);
    }

    public static WildcardType newWildcardType(Type[] lowerBounds, Type[] upperBounds) {
        return new WildcardTypeImpl(lowerBounds, upperBounds);
    }

    private static final Type[] EMPTY_TYPE_ARRAY = new Type[0];
    public static final WildcardType WILDCARD;

    private Types() {
        throw new InternalError();
    }

    public static Type parseType(String typeName) throws ClassNotFoundException {
        return parseType(typeName, Thread.currentThread().getContextClassLoader());
    }

    public static Type parseType(String typeName, ClassLoader classLoader) throws ClassNotFoundException {
        try (Scanner scanner = new Scanner(typeName)) {
            return parseType(scanner, classLoader);
        }
    }

    private static Type parseType(Scanner scanner, ClassLoader classLoader) throws ClassNotFoundException {
        if (!Token.TYPE_NAME.matches(scanner)) {
            throw new IllegalArgumentException("Expected type name in " + scanner);
        }
        return parseArrayType(parseParameterizedType(Types.loadClass(scanner.match().group(1), classLoader), scanner, classLoader), scanner);
    }

    private static Type parseWildcardType(Scanner scanner, ClassLoader classLoader) throws ClassNotFoundException {
        if (!Token.WILDCARD.matches(scanner)) {
            return parseType(scanner, classLoader);
        }
        if (Token.EXTENDS.matches(scanner)) {
            return newWildcardType(EMPTY_TYPE_ARRAY, new Type[]{parseType(scanner, classLoader)});
        }
        if (Token.SUPER.matches(scanner)) {
            return newWildcardType(new Type[]{parseType(scanner, classLoader)}, EMPTY_TYPE_ARRAY);
        }
        return newWildcardType(EMPTY_TYPE_ARRAY, EMPTY_TYPE_ARRAY);
    }

    private static Type parseParameterizedType(Type type, Scanner scanner, ClassLoader classLoader) throws ClassNotFoundException {
        if (!Token.LEFT_ANGLE_BRACKET.matches(scanner)) {
            return type;
        }
        Collection<Type> types = new LinkedList<>();
        do {
            types.add(parseWildcardType(scanner, classLoader));
        } while (Token.COMMA.matches(scanner));
        if (!Token.RIGHT_ANGLE_BRACKET.matches(scanner)) {
            throw new IllegalArgumentException("Expected '>' in " + scanner);
        }
        return newParameterizedType(type, null, types.toArray(EMPTY_TYPE_ARRAY));
    }

    private static Type parseArrayType(Type type, Scanner scanner) {
        int rank = 0;
        while (Token.ARRAY_SQUARE_BRACKETS.matches(scanner)) {
            rank++;
        }
        return arrayType(type, rank);
    }

    private enum Token {

        TYPE_NAME("([._$a-zA-Z0-9]+)"),
        LEFT_ANGLE_BRACKET("<"),
        RIGHT_ANGLE_BRACKET(">"),
        COMMA(","),
        ARRAY_SQUARE_BRACKETS("\\[\\s*]"),
        WILDCARD("\\?"),
        EXTENDS("extends"),
        SUPER("super");

        private final Pattern pattern;

        Token(String regex) {
            pattern = Pattern.compile("\\G\\s*" + regex);
        }

        boolean matches(Scanner scanner) {
            return scanner.findWithinHorizon(pattern, 0) != null;
        }
    }

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
        WILDCARD = newWildcardType(EMPTY_TYPE_ARRAY, EMPTY_TYPE_ARRAY);
    }
}
