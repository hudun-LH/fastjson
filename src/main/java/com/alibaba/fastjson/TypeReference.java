package com.alibaba.fastjson;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.alibaba.fastjson.util.ParameterizedTypeImpl;

/** 
 * Represents a generic type {@code T}. Java doesn't yet provide a way to
 * represent generic types, so this class does. Forces clients to create a
 * subclass of this class which enables retrieval the type information even at
 * runtime.
 *
 * <p>For example, to create a type literal for {@code List<String>}, you can
 * create an empty anonymous inner class:
 *
 * <pre>
 * TypeReference&lt;List&lt;String&gt;&gt; list = new TypeReference&lt;List&lt;String&gt;&gt;() {};
 * </pre>
 * This syntax cannot be used to create type literals that have wildcard
 * parameters, such as {@code Class<?>} or {@code List<? extends CharSequence>}.
 */
public class TypeReference<T> {
    static ConcurrentMap<Class<?>, ConcurrentMap<Type, ConcurrentMap<Type, Type>>> classTypeCache
            = new ConcurrentHashMap<Class<?>, ConcurrentMap<Type, ConcurrentMap<Type, Type>>>(16, 0.75f, 1);

    protected final Type type;

    /**
     * Constructs a new type literal. Derives represented class from type
     * parameter.
     *
     * <p>Clients create an empty anonymous subclass. Doing so embeds the type
     * parameter in the anonymous class's type hierarchy so we can reconstitute it
     * at runtime despite erasure.
     */
    protected TypeReference(){
        Type superClass = getClass().getGenericSuperclass();

        type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
    }
    
    /**
     * @since 1.2.9
     * @param actualTypeArguments
     */
    protected TypeReference(Type... actualTypeArguments){
        Class<?> thisClass = this.getClass();
        Type superClass = thisClass.getGenericSuperclass();

        ParameterizedType argType = (ParameterizedType) ((ParameterizedType) superClass).getActualTypeArguments()[0];
        Type rawType = argType.getRawType();
        Type[] argTypes = argType.getActualTypeArguments();
        
        int actualIndex = 0;
        for (int i = 0; i < argTypes.length; ++i) {
            if (argTypes[i] instanceof TypeVariable) {
                argTypes[i] = actualTypeArguments[actualIndex++];
                if (actualIndex >= actualTypeArguments.length) {
                    break;
                }
            }
        }

        if (actualTypeArguments.length == 1 && argTypes.length == 1) {
            ConcurrentMap<Type, ConcurrentMap<Type, Type>> classCache = classTypeCache.get(thisClass);
            if (classCache == null) {
                classTypeCache.putIfAbsent(thisClass, new ConcurrentHashMap<Type, ConcurrentMap<Type, Type>>(16, 0.75f, 1));
                classCache = classTypeCache.get(thisClass);
            }

            ConcurrentMap<Type, Type> typeCached = classCache.get(argType);
            if (typeCached == null) {
                classCache.putIfAbsent(argType, new ConcurrentHashMap<Type, Type>(16, 0.75f, 1));
                typeCached = classCache.get(argType);
            }

            Type actualTypeArgument = actualTypeArguments[0];

            Type cachedType = typeCached.get(actualTypeArgument);
            if (cachedType == null) {
                typeCached.putIfAbsent(actualTypeArgument, actualTypeArgument);
                cachedType = typeCached.get(actualTypeArgument);
            }

            type = cachedType;
        } else {
            type = new ParameterizedTypeImpl(argTypes, thisClass, rawType);
        }
    }
    
    /**
     * Gets underlying {@code Type} instance.
     */
    public Type getType() {
        return type;
    }
}
