# codegeny-reflexio
This library provides some utility classes for java reflection.

## java.lang.reflect.ParameterizedType expansion

Resolve the type parameters for a specific generic type across its hierarchy.

Example: Let's create some interface `Converter<X, Y>` and try to determine the actual type arguments for that
interface from classes/interfaces which inherit from it (directly or indirectly).

```java
interface Converter<X, Y> {}

interface StringConverter<Z> extends Converter<Z, String> {}

abstract class AbstractStringConverter<A> implements StringConverter<A> {}

class UUIDStringConverter extends AbstractStringConverter<UUID> {}

Types.resolveTypeArguments(Converter.class, Converter.class); // yields [TypeVariable("X"), TypeVariable("Y")]
Types.resolveTypeArguments(StringConverter.class, Converter.class); // yields [TypeVariable("Z"), String.class]
Types.resolveTypeArguments(AbstractStringConverter.class, Converter.class); // yields [TypeVariable("A"), String.class]
Types.resolveTypeArguments(UUIDStringConverter.class, Converter.class); // yields [UUID.class, String.class]
```

For the last example (`UUIDStringConverter`), nowhere in its hierarchy is a type directly implementing
`Converter<UUID, String>` but the method is capable of reconstructing that information.

## java.lang.reflect.Type assignabilty

Assignability checking can be done with or without _capturing_ `TypeVariable`s.
Assignability _with_ capturing should only be used if you need to check assignability to a method/constructor
parameter which is templated.

```java
class MyClass {
    public <S extends CharSequence & Serializable> void doSomething(S text) {}
}

Type left = MyClass.class.getDeclaredMethod("doSomething", CharSequence.class).getGenericParameterTypes[0]; // S
Type right = String.class;

Types.isAssignable(left, right); // return false (not capturing)

Map<TypeVariable<?>, Type> captures = new HashMap<>();
Types.isAssignable(left, right, captures); // return true with ("S", String.class) added to the map
```
    
## java.lang.reflect.Type parsing

```java
Type type = Types.parseType("java.util.Map<? extends java.lang.Number, java.util.Set<? super java.lang.CharSequence>>[]");
```

## java.lang.reflect.Type literal

```java
Type type = new TypeLiteral<Map<? extends Number, Set<? super CharSequence>>[]>() {}.getType();
```

## java.lang.reflect.Type reducing to raw java.lang.Class

```java
Type type = ... // java.util.Set<? extends Number>
Class<?> klass = Types.raw(type); // java.util.Set
```

