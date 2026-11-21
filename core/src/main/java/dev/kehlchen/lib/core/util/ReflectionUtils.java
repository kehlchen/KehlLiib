package dev.kehlchen.lib.core.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

public final class ReflectionUtils {
    private ReflectionUtils() {}
    public static Optional<Field> findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try { return Optional.of(current.getDeclaredField(name)); }
            catch (NoSuchFieldException ignored) { current = current.getSuperclass(); }
        }
        return Optional.empty();
    }
    public static Optional<Method> findMethod(Class<?> type, String name, Class<?>... parameters) {
        try { return Optional.of(type.getDeclaredMethod(name, parameters)); }
        catch (NoSuchMethodException e) { return Optional.empty(); }
    }
    public static Object readField(Object target, String name) {
        Field field = findField(target.getClass(), name).orElseThrow(() -> new NoSuchFieldError(name));
        try { field.setAccessible(true); return field.get(target); }
        catch (IllegalAccessException e) { throw new IllegalStateException(e); }
    }
    public static void writeField(Object target, String name, Object value) {
        Field field = findField(target.getClass(), name).orElseThrow(() -> new NoSuchFieldError(name));
        try { field.setAccessible(true); field.set(target, value); }
        catch (IllegalAccessException e) { throw new IllegalStateException(e); }
    }
    public static Object invoke(Object target, Method method, Object... arguments) {
        try { method.setAccessible(true); return method.invoke(target, arguments); }
        catch (IllegalAccessException | InvocationTargetException e) { throw new IllegalStateException(e); }
    }
}
