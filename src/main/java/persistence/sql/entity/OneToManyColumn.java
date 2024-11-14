package persistence.sql.entity;

import jakarta.persistence.OneToMany;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;

public class OneToManyColumn {
    private final EntityColumn entityColumn;
    private final Field field;

    public OneToManyColumn(EntityColumn entityColumn, Field field) {
        this.entityColumn = entityColumn;
        this.field = field;
    }
    public String getForeignKeyColumnName() {
        if (field.isAnnotationPresent(OneToMany.class)) {
            OneToMany oneToManyAnnotation = field.getAnnotation(OneToMany.class);
            return field.getName() + "_id";
        }
        return null;
    }

    public Class<?> getJoinEntityClass() {
        return (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
    }

}
