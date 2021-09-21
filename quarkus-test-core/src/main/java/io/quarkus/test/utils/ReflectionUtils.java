package io.quarkus.test.utils;

import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ReflectionUtils {

    private ReflectionUtils() {

    }

    public static <T> T getStaticFieldValue(Field field) {
        try {
            field.setAccessible(true);
            return (T) field.get(null);
        } catch (IllegalAccessException e) {
            fail("Can't resolve field value. Fields need to be in static. Problematic field: " + field.getName());
        }

        return null;
    }

    public static void setStaticFieldValue(Field field, Object value) {
        field.setAccessible(true);
        if (Modifier.isStatic(field.getModifiers())) {
            try {
                field.set(null, value);
            } catch (IllegalAccessException e) {
                fail("Couldn't set value. Fields can only be injected into static instances. Problematic field: " + field
                        .getName());
            }
        } else {
            fail("Fields can only be injected into static instances. Problematic field: " + field.getName());
        }
    }

    public static List<Field> findAllFields(Class<?> clazz) {
        if (clazz == Object.class) {
            return Collections.emptyList();
        }

        List<Field> fields = new ArrayList<>();
        fields.addAll(findAllFields(clazz.getSuperclass()));
        fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
        return fields;
    }

    public static <T> T createInstance(Class<T> clazz, Object... args) {
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (constructor.getParameterCount() == args.length) {
                try {
                    return (T) constructor.newInstance(args);
                } catch (Exception ex) {
                    fail("Constructor failed to be called. Caused by " + ex.getMessage());
                }
            }
        }

        fail("Constructor not found for " + clazz);
        return null;
    }

    public static Object invokeMethod(Object instance, String methodName, Object... args) {
        for (Method method : instance.getClass().getMethods()) {
            if (methodName.equals(method.getName())) {
                return org.junit.platform.commons.util.ReflectionUtils.invokeMethod(method, instance, args);
            }
        }

        fail("Method " + methodName + " not found in " + instance.getClass());
        return null;
    }
}
