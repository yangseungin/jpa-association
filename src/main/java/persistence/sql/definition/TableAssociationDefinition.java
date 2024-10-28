package persistence.sql.definition;

import common.ReflectionFieldAccessUtils;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;

public class TableAssociationDefinition {
    private final TableDefinition associatedTableDefinition;
    private JoinColumn joinColumn;
    private final FetchType fetchType;
    private final String fieldName;

    public TableAssociationDefinition(
            Class<?> associatedEntityClass,
            Field field) {
        this.joinColumn = field.getAnnotation(JoinColumn.class);
        this.associatedTableDefinition = new TableDefinition(associatedEntityClass);
        this.fieldName = field.getName();
        this.fetchType = getFetchType(field);
    }

    private static FetchType getFetchType(Field field) {
        if (field.isAnnotationPresent(OneToMany.class)) {
            return field.getAnnotation(OneToMany.class).fetch();
        }

        if (field.isAnnotationPresent(ManyToMany.class)) {
            return field.getAnnotation(ManyToMany.class).fetch();
        }

        return FetchType.EAGER;
    }

    public TableDefinition getAssociatedTableDefinition() {
        return associatedTableDefinition;
    }

    public Class<?> getAssociatedEntityClass() {
        return associatedTableDefinition.getEntityClass();
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getTableName() {
        return associatedTableDefinition.getTableName();
    }

    public boolean isFetchEager() {
        return fetchType == FetchType.EAGER;
    }

    public String getJoinColumnName() {
        if (joinColumn != null) {
            return joinColumn.name();
        }
        return "";
    }

    public Collection<Object> getCollectionField(Object instance) throws NoSuchFieldException {
        final Field field = instance.getClass().getDeclaredField(getFieldName());
        Collection<Object> entityCollection = (Collection<Object>) ReflectionFieldAccessUtils.accessAndGet(instance, field);
        if (entityCollection == null) {
            entityCollection = new ArrayList<>();
            ReflectionFieldAccessUtils.accessAndSet(instance, field, entityCollection);
        }

        return entityCollection;
    }
}
