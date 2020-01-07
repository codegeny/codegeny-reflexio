package org.codegeny.reflexio;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.util.Set;
import java.util.function.Supplier;

import static org.codegeny.reflexio.Types.methodTypeVariable;

public class RawClassResolverTest {

    public Supplier<?> x;
    public Supplier<? extends Serializable> y;

    @Test
    public <A, B extends Serializable, C extends Number & CharSequence> void test() throws Exception {

        TypeVariable<?> tpa = methodTypeVariable("A", RawClassResolverTest.class, "test");
        TypeVariable<?> tpb = methodTypeVariable("B", RawClassResolverTest.class, "test");
        TypeVariable<?> tpc = methodTypeVariable("C", RawClassResolverTest.class, "test");


        Assertions.assertEquals(Object.class, Types.raw(tpa));
        Assertions.assertEquals(Serializable.class, Types.raw(tpb));
        Assertions.assertEquals(Number.class, Types.raw(tpc));

        Assertions.assertEquals(Object.class, Types.raw(((ParameterizedType) RawClassResolverTest.class.getField("x").getGenericType()).getActualTypeArguments()[0]));
        Assertions.assertEquals(Serializable.class, Types.raw(((ParameterizedType) RawClassResolverTest.class.getField("y").getGenericType()).getActualTypeArguments()[0]));

    }

    @Test
    public void test2() throws ClassNotFoundException {
        Assertions.assertEquals(Set[][][].class, Types.raw(Types.parseType("java.util.Set<?>[][][]")));
    }
}
