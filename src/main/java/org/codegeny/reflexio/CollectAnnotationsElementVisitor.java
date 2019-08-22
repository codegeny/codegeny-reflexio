package org.codegeny.reflexio;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

class CollectAnnotationsElementVisitor<A extends Annotation> implements AnnotatedElementVisitor<Set<A>> {

    private final Set<AnnotatedElement> visited = new HashSet<>();
    private final Set<A> set = new LinkedHashSet<>();
    private final Class<A> annotationType;

    public CollectAnnotationsElementVisitor(Class<A> annotationType) {
        this.annotationType = annotationType;
    }

    private Set<A> add(AnnotatedElement element, Supplier<Stream<AnnotatedElement>> next) {
        if (visited.add(element)) {
            if (element.isAnnotationPresent(annotationType)) {
                set.add(element.getAnnotation(annotationType));
            } else {
                set.addAll(Arrays.asList(element.getAnnotationsByType(annotationType)));
            }
            next.get().forEach(t -> AnnotatedElementVisitor.accept(this, t));
        }
        return set;
    }

    @Override
    public <T> Set<A> visit(Class<T> klass) {
        return add(klass, () -> Stream.concat(Stream.of(klass.getInterfaces()), Stream.of(klass.getPackage(), klass.getSuperclass())));
    }

    @Override
    public <T> Set<A> visit(Constructor<T> constructor) {
        return add(constructor, () -> Stream.of(constructor.getDeclaringClass()));
    }

    @Override
    public Set<A> visit(Method method) {
        return add(method, () -> Stream.of(method.getDeclaringClass(), method.getReturnType()));
    }

    @Override
    public Set<A> visit(Field field) {
        return add(field, () -> Stream.of(field.getDeclaringClass(), field.getType()));
    }

    @Override
    public Set<A> visit(Parameter parameter) {
        return add(parameter, () -> Stream.of(parameter.getDeclaringExecutable(), parameter.getType()));
    }

    @Override
    public Set<A> visit(Package pakkage) {
        return add(pakkage, Stream::empty);
    }
}
