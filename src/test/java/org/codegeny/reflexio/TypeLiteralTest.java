package org.codegeny.reflexio;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

public class TypeLiteralTest {

    @Test
    public void test() throws ClassNotFoundException {
        Type left = new TypeLiteral<Map<? super Number[], Set<? extends CharSequence>[]>>() {}.getType();
        Type right = Types.parseType("java.util.Map<? super java.lang.Number[], java.util.Set<? extends java.lang.CharSequence>[]>");
        Assertions.assertEquals(right, left);
        Assertions.assertEquals(left, right);
        Assertions.assertEquals(right.hashCode(), left.hashCode());
    }
}
