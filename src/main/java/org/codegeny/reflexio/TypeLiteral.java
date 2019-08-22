package org.codegeny.reflexio;

import java.lang.reflect.Type;
import java.util.Objects;

public abstract class TypeLiteral<T> {

    private final Type type;

    protected TypeLiteral() {
        type = Objects.requireNonNull(Types.resolveTypeArguments(getClass(), TypeLiteral.class)[0]);
    }

    public final Type getType() {
        return type;
    }

    @SuppressWarnings("unchecked")
    public final T cast(Object object) {
        return (T) object;
    }

    @Override
    public final boolean equals(Object that) {
        return super.equals(that) || that instanceof TypeLiteral<?> && type.equals(((TypeLiteral<?>) that).type);
    }

    @Override
    public final int hashCode() {
        return type.hashCode();
    }

    @Override
    public final String toString() {
        return type.toString();
    }
}
