package org.codegeny.reflexio;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TypeParserTest {

    @Test
    public void otherTest() {
        Assertions.assertEquals(Object[][][][].class, Types.arrayType(Object[][].class, 2));
    }

    @Test
    public void test() throws ClassNotFoundException {
        parse("byte");
        parse("byte[][][]");
        parse("java.lang.Object");
        parse("java.lang.Object[]");
        parse("java.util.Set");
        parse("java.util.Set[][]");
        parse("java.util.Set<java.lang.Integer>");
        parse("java.util.Set<?>");
        parse("java.util.Set<? extends java.lang.Integer>");
        parse("java.util.Set<? super java.lang.Integer>");
        parse("java.util.Set<java.lang.Integer[][]>[]");
        parse("java.util.Map<java.util.Set<java.lang.Integer[]>[],java.lang.Long[][]>[]");
        parse("java.util.Map  <   java.util.Set  <  java.lang.Integer []  >  []  ,  long  []  []  >   []");
    }

    private void parse(String string) throws ClassNotFoundException {
        Assertions.assertEquals(
                string.replaceAll("\\s+", ""),
                Types.parseType(string).getTypeName().replaceAll("\\s+", "")
        );
    }
}
