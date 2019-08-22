package org.codegeny.reflexio;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Parser for java type names. Does not support {@link java.lang.reflect.TypeVariable}s which need a
 * {@link java.lang.reflect.GenericDeclaration} (which cannot be represented in the string) and because they are
 * indistinguishable from other types in a string form.
 *
 * @author Xavier DURY
 */
public final class TypeParser {

    public static TypeParser newInstance() {
        return newInstance(Thread.currentThread().getContextClassLoader());
    }

    public static TypeParser newInstance(ClassLoader classLoader) {
        return new TypeParser(classLoader);
    }

    private final ClassLoader classLoader;

    private TypeParser(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public Type parseType(String typeName) throws ClassNotFoundException {
        try (Scanner scanner = new Scanner(typeName)) {
            return parseType(scanner);
        }
    }

    private Type parseType(Scanner scanner) throws ClassNotFoundException {
        if (!Token.TYPE_NAME.matches(scanner)) {
            throw new IllegalArgumentException("Expected type name in " + scanner);
        }
        return parseArrayType(parseParameterizedType(Types.loadClass(scanner.match().group(1), classLoader), scanner), scanner);
    }

    private Type parseWildcardType(Scanner scanner) throws ClassNotFoundException {
        if (!Token.WILDCARD.matches(scanner)) {
            return parseType(scanner);
        }
        if (Token.EXTENDS.matches(scanner)) {
            return Types.newWildcardType(EMPTY_TYPE_ARRAY, new Type[]{parseType(scanner)});
        }
        if (Token.SUPER.matches(scanner)) {
            return Types.newWildcardType(new Type[]{parseType(scanner)}, EMPTY_TYPE_ARRAY);
        }
        return Types.newWildcardType(EMPTY_TYPE_ARRAY, EMPTY_TYPE_ARRAY);
    }

    private Type parseParameterizedType(Type type, Scanner scanner) throws ClassNotFoundException {
        if (!Token.LEFT_ANGLE_BRACKET.matches(scanner)) {
            return type;
        }
        Collection<Type> types = new LinkedList<>();
        do {
            types.add(parseWildcardType(scanner));
        } while (Token.COMMA.matches(scanner));
        if (!Token.RIGHT_ANGLE_BRACKET.matches(scanner)) {
            throw new IllegalArgumentException("Expected '>' in " + scanner);
        }
        return Types.newParameterizedType(type, null, types.toArray(EMPTY_TYPE_ARRAY));
    }

    private Type parseArrayType(Type type, Scanner scanner) {
        int dimension = 0;
        while (Token.ARRAY_SQUARE_BRACKETS.matches(scanner)) {
            dimension++;
        }
        return Types.arrayType(type, dimension);
    }

    private static final Type[] EMPTY_TYPE_ARRAY = new Type[0];

    enum Token {

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
}
