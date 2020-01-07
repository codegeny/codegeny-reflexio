package org.codegeny.reflexio;

import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class WildcardTypeImpl implements WildcardType {

    private static final Type[] DEFAULT_UPPER_BOUNDS = new Type[]{Object.class};

    private final Type[] lowerBounds;
    private final Type[] upperBounds;

    WildcardTypeImpl(Type[] lowerBounds, Type[] upperBounds) {
        this.lowerBounds = Objects.requireNonNull(lowerBounds);
        this.upperBounds = Objects.requireNonNull(upperBounds);
    }

    @Override
    public Type[] getUpperBounds() {
        return upperBounds.length == 0 ? DEFAULT_UPPER_BOUNDS : upperBounds;
    }

    @Override
    public Type[] getLowerBounds() {
        return lowerBounds;
    }

    @Override
    public boolean equals(Object that) {
        return super.equals(that) || that instanceof WildcardType && equals((WildcardType) that);
    }

    private boolean equals(WildcardType that) {
        return Arrays.equals(getLowerBounds(), that.getLowerBounds()) && Arrays.equals(getUpperBounds(), that.getUpperBounds());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getLowerBounds()) ^ Arrays.hashCode(getUpperBounds());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("?");
        if (lowerBounds.length > 0) {
            builder.append(" super ").append(Stream.of(lowerBounds).map(Type::getTypeName).collect(Collectors.joining(", ")));
        }
        if (upperBounds.length > 0 && !Arrays.equals(upperBounds, DEFAULT_UPPER_BOUNDS)) {
            builder.append(" extends ").append(Stream.of(upperBounds).map(Type::getTypeName).collect(Collectors.joining(", ")));
        }
        return builder.toString();
    }
}
