package common;

import java.lang.reflect.Field;

public class ReflectionFieldAccessUtils {
    private ReflectionFieldAccessUtils() {
    }

    public static Object accessAndGet(Object target, Field field) {
        final boolean wasAccessible = field.canAccess(target);
        try {
            if (!wasAccessible) {
                field.setAccessible(true);
            }

            return field.get(target);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot access field value", e);
        } finally {
            if (!wasAccessible) {
                field.setAccessible(false);
            }
        }
    }

    public static void accessAndSet(Object target, Field field, Object value) {
        final boolean wasAccessible = field.canAccess(target);
        try {
            if (!wasAccessible) {
                field.setAccessible(true);
            }

            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot access field value", e);
        } finally {
            if (!wasAccessible) {
                field.setAccessible(false);
            }
        }
    }
}
