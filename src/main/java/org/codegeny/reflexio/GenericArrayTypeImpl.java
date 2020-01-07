package org.codegeny.reflexio;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.Objects;

final class GenericArrayTypeImpl implements GenericArrayType {

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
        return Objects.hashCode(component);
    }

    @Override
    public String toString() {
        return component.getTypeName().concat("[]");
    }
}
