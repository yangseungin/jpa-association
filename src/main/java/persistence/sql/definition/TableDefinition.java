package persistence.sql.definition;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.jetbrains.annotations.NotNull;
import persistence.sql.Queryable;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class TableDefinition {

    private final String tableName;
    private final Class<?> entityClass;
    private final List<TableColumn> columns;
    private final List<TableAssociationDefinition> associations;
    private final TableId tableId;

    public TableDefinition(Class<?> entityClass) {
        validate(entityClass);

        final Field[] fields = entityClass.getDeclaredFields();

        this.tableName = determineTableName(entityClass);
        this.entityClass = entityClass;
        this.associations = determineAssociations(entityClass);
        this.columns = createTableColumns(fields);
        this.tableId = new TableId(fields);
    }

    @NotNull
    private static List<TableAssociationDefinition> determineAssociations(Class<?> entityClass) {
        final List<Field> collectionFields = Arrays.stream(entityClass.getDeclaredFields())
                .filter(field -> Collection.class.isAssignableFrom(field.getType()))
                .toList();

        if (collectionFields.isEmpty()) {
            return List.of();
        }

        return collectionFields.stream()
                .map(field -> new TableAssociationDefinition(getGenericActualType(field), field))
                .toList();
    }

    @NotNull
    private static Class<?> getGenericActualType(Field field) {
        final Type genericType = field.getGenericType();
        final Type[] actualTypeArguments = ((ParameterizedType) genericType).getActualTypeArguments();

        return (Class<?>) actualTypeArguments[0];
    }

    @NotNull
    private static String determineTableName(Class<?> entityClass) {
        final String tableName = entityClass.getSimpleName();

        if (entityClass.isAnnotationPresent(Table.class)) {
            Table tableAnnotation = entityClass.getAnnotation(Table.class);
            if (!tableAnnotation.name().isEmpty()) {
                return tableAnnotation.name();
            }
        }

        if (entityClass.isAnnotationPresent(Entity.class)) {
            Entity entityAnnotation = entityClass.getAnnotation(Entity.class);
            if (!entityAnnotation.name().isEmpty()) {
                return entityAnnotation.name();
            }
        }

        return tableName;
    }

    private static List<TableColumn> createTableColumns(Field[] fields) {
        return Arrays.stream(fields)
                .filter(field -> !field.isAnnotationPresent(Id.class))
                .filter(field -> !field.isAnnotationPresent(Transient.class))
                .filter(field -> !Collection.class.isAssignableFrom(field.getType()))
                .map(TableColumn::new)
                .toList();
    }

    public void validate(Class<?> clazz) {
        if (!clazz.isAnnotationPresent(Entity.class)) {
            throw new IllegalArgumentException("Entity must be annotated with @Entity");
        }

        validateHasId(clazz.getDeclaredFields());
    }

    private void validateHasId(Field[] fields) {
        List<Field> idFields = Arrays.stream(fields)
                .filter(field ->
                        field.isAnnotationPresent(Id.class)
                ).toList();

        if (idFields.size() != 1) {
            throw new IllegalArgumentException("Entity must have exactly one field annotated with @Id");
        }
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public TableId getTableId() {
        return tableId;
    }

    public Serializable getIdValue(Object entity) {
        return (Serializable) tableId.getValue(entity);
    }

    public boolean hasId(Object entity) {
        return tableId.hasValue(entity);
    }

    public String getTableName() {
        return tableName;
    }

    public List<? extends Queryable> withIdColumns() {
        return Stream.concat(
                        Stream.of(tableId),
                        columns.stream()
                )
                .toList();
    }

    public List<? extends Queryable> withoutIdColumns() {
        return columns;
    }

    public List<? extends Queryable> hasValueColumns(Object entity) {
        return withIdColumns().stream()
                .filter(column -> column.hasValue(entity))
                .toList();
    }

    public List<TableAssociationDefinition> getAssociations() {
        return associations;
    }

    public boolean hasAssociations() {
        return !associations.isEmpty();
    }

    public boolean hasColumn(String name) {
        return columns.stream()
                .anyMatch(column -> column.getColumnName().equals(name));
    }

    public Queryable getColumn(String name) {
        return withIdColumns().stream()
                .filter(column -> column.getColumnName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Column not found"));
    }
}
