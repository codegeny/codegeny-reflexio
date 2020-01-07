package org.codegeny.reflexio;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class ParameterizedTypeImpl implements ParameterizedType {

    private final Type rawType;
    private final Type ownerType;
    private final Type[] arguments;

    ParameterizedTypeImpl(Type rawType, Type ownerType, Type... arguments) {
        this.rawType = Objects.requireNonNull(rawType);
        this.ownerType = ownerType;
        this.arguments = Objects.requireNonNull(arguments);
    }

    @Override
    public Type getRawType() {
        return rawType;
    }

    @Override
    public Type getOwnerType() {
        return ownerType;
    }

    @Override
    public Type[] getActualTypeArguments() {
        return arguments;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(rawType)
                ^ Objects.hashCode(ownerType)
                ^ Arrays.hashCode(arguments);
    }

    @Override
    public boolean equals(Object that) {
        return super.equals(that) || that instanceof ParameterizedType && equals((ParameterizedType) that);
    }

    private boolean equals(ParameterizedType that) {
        return Objects.equals(rawType, that.getRawType())
                && Objects.equals(ownerType, that.getOwnerType())
                && Arrays.equals(arguments, that.getActualTypeArguments());
    }

    @Override
    public String toString() {
        return rawType.getTypeName().concat(Stream.of(getActualTypeArguments()).map(Type::getTypeName).collect(Collectors.joining(", ", "<", ">")));
    }
}
