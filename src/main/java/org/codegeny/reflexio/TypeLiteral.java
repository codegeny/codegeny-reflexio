package org.codegeny.reflexio;

import java.lang.reflect.Type;
import java.util.Objects;

/**
 * Class used to <em>capture</em>capture a type literal. To capture a type literal, this class should be extended and
 * parameterized with the correct type.
 *
 * @param <T> The type to capture.
 * @author Xavier DURY
 */
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
    public final String toString() {
        return type.toString();
    }
}
