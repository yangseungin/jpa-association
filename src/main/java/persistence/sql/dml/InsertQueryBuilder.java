package persistence.sql.dml;

import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import persistence.sql.entity.EntityColumn;
import persistence.sql.entity.EntityColumns;
import persistence.sql.entity.EntityTable;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class InsertQueryBuilder {

    public String getInsertQuery(EntityTable entityTable, EntityColumns entityColumns, Object object, Object parentEntity) {
        String tableName = entityTable.getTableName();
        String tableColumns = columnsClause(entityColumns, parentEntity);
        String tableValues = valueClause(entityColumns, object, getIdValue(parentEntity));
        return String.format("insert into %s (%s) VALUES (%s)", tableName, tableColumns, tableValues);
    }

    private String columnsClause(EntityColumns entityColumns, Object parentEntity) {
        List<String> columns = entityColumns.getColumns().stream()
                .filter(column -> !column.isGeneratedValue())
                .filter(column -> !column.isTransient())
                .filter(column -> !column.isOneToMany())
                .map(EntityColumn::getColumnName)
                .collect(Collectors.toList());

        if (parentEntity != null) {
            columns.add(getFKColumnFromParentEntity(parentEntity));
        }

        return String.join(", ", columns);
    }

    private String getFKColumnFromParentEntity(Object parentEntity) {
        try {
            Field oneToManyField = Arrays.stream(parentEntity.getClass().getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(OneToMany.class))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("OneToMany 필드가 없음"));

            JoinColumn joinColumn = oneToManyField.getAnnotation(JoinColumn.class);
            if (joinColumn != null) {
                return joinColumn.name();
            }

            return oneToManyField.getName() + "_id";
        } catch (Exception e) {
            throw new RuntimeException("부모 엔티티로부터 외래키를 추출할 수 없음", e);
        }
    }

    private String valueClause(EntityColumns entityColumns, Object object, Long parentId) {
        List<String> values = entityColumns.getColumns().stream()
                .filter(column -> !column.isGeneratedValue())
                .filter(column -> !column.isTransient())
                .filter(column -> !column.isOneToMany())
                .map(column -> {
                    if (column.isOneToMany()) {
                        return parentId != null ? parentId.toString() : "null";
                    }
                    return column.getFieldValue(object);
                })
                .collect(Collectors.toList());

        if (parentId != null) {
            values.add(String.valueOf(parentId));
        }

        return String.join(", ", values);
    }

    public Long getIdValue(Object entity) {
        if (entity != null) {
            Class<?> clazz = entity.getClass();

            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    field.setAccessible(true);
                    try {
                        return (Long) field.get(entity);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("id값이 없음", e);
                    }
                }
            }
        }

        return null;
    }
}
