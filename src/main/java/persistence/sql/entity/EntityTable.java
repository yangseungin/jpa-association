package persistence.sql.entity;

import jakarta.persistence.Table;

public class EntityTable {
    private final Class<?> entityClass;
    private final String tableName;

    public EntityTable(Class<?> entityClass, String tableName) {
        this.entityClass = entityClass;
        this.tableName = tableName;
    }

    public static EntityTable from(Class<?> clazz) {
        return new EntityTable(clazz, extractTableName(clazz));
    }

    private static String extractTableName(Class<?> clazz) {
        Table annotation = clazz.getAnnotation(Table.class);
        if (annotation == null) {
            return clazz.getSimpleName().toLowerCase();
        }

        if (!annotation.name().isEmpty()) {
            return annotation.name();
        }

        return clazz.getSimpleName().toLowerCase();
    }

    public String getTableName() {
        return tableName;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }
}
