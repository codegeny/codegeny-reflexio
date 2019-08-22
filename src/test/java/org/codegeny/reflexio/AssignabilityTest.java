package org.codegeny.reflexio;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.codegeny.reflexio.Types.isAssignable;
import static org.codegeny.reflexio.Types.methodTypeVariable;
import static org.codegeny.reflexio.Types.newGenericArrayType;
import static org.codegeny.reflexio.Types.newParameterizedType;
import static org.codegeny.reflexio.Types.newWildcardType;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AssignabilityTest {

    private interface Provider<A, B> extends Supplier<B> {
    }

    @Test
    public <A, B extends Number, C extends Collection<A>, Z extends A, T extends Comparable<T>> void klass() {

        TypeVariable<?> tpa = methodTypeVariable("A", AssignabilityTest.class, "klass");
        TypeVariable<?> tpb = methodTypeVariable("B", AssignabilityTest.class, "klass");
        TypeVariable<?> tpc = methodTypeVariable("C", AssignabilityTest.class, "klass");
        TypeVariable<?> tpz = methodTypeVariable("Z", AssignabilityTest.class, "klass");
        TypeVariable<?> tpt = methodTypeVariable("T", AssignabilityTest.class, "klass");


        // classes

        this.<Object, String>assertAssignable(Object.class, String.class, (a, b) -> a = b);
        this.<Collection, Set<A>>assertAssignable(Collection.class, newParameterizedType(Set.class, null, tpa), (a, b) -> a = b);
        this.<Number, B>assertAssignable(Number.class, tpb, (a, b) -> a = b);
        this.<Number[], B[]>assertAssignable(Number[].class, newGenericArrayType(tpb), (a, b) -> a = b);
        this.<List<Collection>, List<? extends Collection>>assertAssignable(
                newParameterizedType(List.class, null, Collection.class),
                newParameterizedType(List.class, null, newWildcardType(new Type[0], new Type[] {Collection.class})),
                (a, b) -> { Collection z = a.get(0); z = b.get(0); },
                (a, b) -> a = b
        );
        this.assertAssignable(long[].class, byte[].class);

        // parameterizedTypes

        this.<Collection<A>, Set>assertAssignable(newParameterizedType(Collection.class, null, tpa), Set.class, (a, b) -> a = b);
        this.<Collection<? extends A>, Set<A>>assertAssignable(newParameterizedType(Collection.class, null, newWildcardType(new Type[0], new Type[]{tpa})), newParameterizedType(Set.class, null, tpa), (a, b) -> a = b);
        this.<Collection<A>, C>assertAssignable(newParameterizedType(Collection.class, null, tpa), tpc, (a, b) -> a = b);
        /*
        this.<List<Collection<A>>, List<? extends Collection<A>>>assertAssignable(
                newParameterizedType(List.class, null, newParameterizedType(Collection.class, null, tpa)),
                newParameterizedType(List.class, null, newWildcardType(new Type[0], new Type[] {newParameterizedType(Collection.class, null, tpa)})),
                (a, b) -> { Collection<A> z = a.get(0); z = b.get(0); },
                (a, b) -> a = b
                );

         */

        // genericArray

        this.<A[], Z[]>assertAssignable(newGenericArrayType(tpa), newGenericArrayType(tpz), (a, b) -> a = b);


        // wildcards

        this.<Supplier<? extends CharSequence>[], Provider<?, String>[]>assertAssignable(
                newGenericArrayType(newParameterizedType(Supplier.class, null, newWildcardType(new Type[0], new Type[]{CharSequence.class}))),
                newGenericArrayType(newParameterizedType(Provider.class, null, newWildcardType(new Type[0], new Type[]{Object.class}), String.class)),
                (a, b) -> a = b
        );

        this.assertAssignable(
                methodTypeVariable("A", AssignabilityTest.class, "klass")
        );

        this.assertAssignable(
                methodTypeVariable("A", AssignabilityTest.class, "klass"),
                methodTypeVariable("B", AssignabilityTest.class, "klass")
        );

        this.<Number, B>assertAssignable(
                Number.class,
                methodTypeVariable("B", AssignabilityTest.class, "klass"),
                (a, b) -> a = b
        );

        this.<Object[], A[]>assertAssignable(
                Object[].class,
                newGenericArrayType(methodTypeVariable("A", AssignabilityTest.class, "klass")),
                (a, b) -> a = b
        );

        this.<Collection<? extends Number>, Set<? extends Long>>assertAssignable(
                newParameterizedType(Collection.class, null, newWildcardType(new Type[0], new Type[]{Number.class})),
                newParameterizedType(Set.class, null, newWildcardType(new Type[0], new Type[]{Long.class})),
                (a, b) -> a = b
        );

        this.<A, Z>assertAssignable(tpa, tpz, (a, b) -> a = b);

    }

    private <A, B> void assertAssignable(Type a, Type b, BiConsumer<A, B> test) {
        assertTrue(isAssignable(a, b));
        assertFalse(isAssignable(b, a));
    }

    private <A, B> void assertAssignable(Type a, Type b, BiConsumer<A, B> test, BiConsumer<B, A> test2) {
        assertTrue(isAssignable(a, b));
        assertTrue(isAssignable(b, a));
    }

    private void assertAssignable(Type a) {
        assertTrue(isAssignable(a, a));
    }

    private void assertAssignable(Type a, Type b) {
        assertFalse(isAssignable(a, b));
        assertFalse(isAssignable(b, a));
    }
}
