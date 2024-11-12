package persistence.sql.entity;

import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import jdbc.RowMapper;
import persistence.sql.Metadata;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EntityRowMapper<T> implements RowMapper<T> {
    private final Class<T> clazz;

    public EntityRowMapper(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public T mapRow(ResultSet resultSet) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, SQLException {
        Metadata metadata = new Metadata(clazz);
        T entity = clazz.getDeclaredConstructor().newInstance();
        for (Field field : clazz.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Transient.class) && !field.isAnnotationPresent(OneToMany.class)) {
                field.setAccessible(true);
                field.set(entity, resultSet.getObject(metadata.getFieldName(field)));
            }
        }

        for (Field field : clazz.getDeclaredFields()) {
            if (List.class.isAssignableFrom(field.getType())) {
                List<Object> children = mapChildEntities(resultSet, field, metadata);
                field.setAccessible(true);
                field.set(entity, children);
            }
        }


        return entity;

    }

    private List<Object> mapChildEntities(ResultSet resultSet, Field listField, Metadata metadata) throws SQLException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        List<Object> children = new ArrayList<>();
        Class<?> childType = (Class<?>) ((ParameterizedType) listField.getGenericType()).getActualTypeArguments()[0];

        do {
            Object childEntity = childType.getDeclaredConstructor().newInstance();

            for (Field childField : childType.getDeclaredFields()) {
                if (!childField.isAnnotationPresent(Transient.class)) {
                    childField.setAccessible(true);
                    String fieldName = metadata.getFieldName(childField);
                    childField.set(childEntity, resultSet.getObject(fieldName));
                }
            }

            children.add(childEntity);
        } while (resultSet.next());

        return children;
    }
}

