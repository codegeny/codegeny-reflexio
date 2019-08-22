package org.codegeny.reflexio;

import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.codegeny.reflexio.TypeAssertions.isTypeVariable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TypeVariableExpanderTest {

    interface Provider extends Supplier<Integer> {
    }

    interface SupplierOfSupplier<E> extends Supplier<Supplier<E>> {
    }

    interface Provider2 extends SupplierOfSupplier<Long> {
    }

    @Test
    public void test() throws Exception {
        assertEquals(Integer.class, Types.expand(Provider.class.getMethod("get"), Provider.class));
        assertEquals(Integer.class, Types.expand(Supplier.class.getMethod("get"), Provider.class));
        assertTrue(isTypeVariable(Types.expand(Supplier.class.getMethod("get"), Supplier.class), "T"));
        assertEquals(Types.newParameterizedType(Supplier.class, null, Long.class), Types.expand(Provider2.class.getMethod("get"), Provider2.class));
    }
}
