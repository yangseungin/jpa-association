package persistence.sql.entity;

import jakarta.persistence.OneToMany;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;

public class OneToManyColumn {
    private final Field field;

    public OneToManyColumn(Field field) {
        this.field = field;
    }

    public Class<?> getJoinEntityClass() {
        return (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
    }

}
