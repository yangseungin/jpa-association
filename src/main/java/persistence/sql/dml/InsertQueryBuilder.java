package persistence.sql.dml;

import jakarta.persistence.Id;
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
            EntityColumn oneToManyColumn = getOneToManyColumnFromParentEntity(parentEntity);
            if (oneToManyColumn != null) {
                columns.add(oneToManyColumn.getOneToManyColumn().getForeignKeyColumnName());
            }
        }

        return String.join(", ", columns);
    }
    private EntityColumn getOneToManyColumnFromParentEntity(Object parentEntity) {
        for (Field field : parentEntity.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(OneToMany.class)) {
                return EntityColumn.from(field);
            }
        }

        return null;
    }

    private Field findOneToManyField(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(OneToMany.class))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("OneToMany 필드를 찾을 수 없습니다."));
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
