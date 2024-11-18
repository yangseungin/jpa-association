package persistence.sql.entity;

import jakarta.persistence.Id;
import jdbc.JdbcTemplate;
import persistence.sql.dml.DeleteQueryBuilder;
import persistence.sql.dml.InsertQueryBuilder;
import persistence.sql.dml.UpdateQueryBuilder;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.List;

public class EntityPersister {

    private final EntityTable entityTable;
    private final EntityColumns entityColumns;
    private final JdbcTemplate jdbcTemplate;

    private final UpdateQueryBuilder updateQueryBuilder;
    private final InsertQueryBuilder insertQueryBuilder;
    private final DeleteQueryBuilder deleteQueryBuilder;

    public EntityPersister(Class<?> clazz, Connection connection) {
        this.entityTable = EntityTable.from(clazz);
        this.entityColumns = EntityColumns.from(clazz);
        this.jdbcTemplate = new JdbcTemplate(connection);

        this.updateQueryBuilder = new UpdateQueryBuilder();
        this.insertQueryBuilder = new InsertQueryBuilder();
        this.deleteQueryBuilder = new DeleteQueryBuilder();
    }

    public boolean update(Object entity) {
        try {
            String updateQuery = updateQueryBuilder.update(entityTable, entityColumns, entity, getIdValue(entity));
            jdbcTemplate.execute(updateQuery);
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    public void insert(Object entity) {
        insertEntity(entity);

        insertChildEntities(entity);
    }

    private Long insertEntity(Object entity) {
        String insertQuery = insertQueryBuilder.getInsertQuery(entityTable, entityColumns, entity, null);
        Long idValue = jdbcTemplate.insertAndReturnId(insertQuery);
        setIdValue(entity, idValue);
        return idValue;
    }

    private void insertChildEntities(Object entity) {
        List<EntityColumn> oneToManyColumns = entityColumns.getOneToManyColumns();

        for (EntityColumn oneToManyColumn : oneToManyColumns) {
            if (oneToManyColumn.isOneToMany()) {
                List<?> children = getChildren(entity, oneToManyColumn);
                for (Object child : children) {
                    insertChildEntity(child, entity);
                }
            }
        }
    }

    private List<?> getChildren(Object entity, EntityColumn oneToManyColumn) {
        try {
            Field field = oneToManyColumn.getField();
            field.setAccessible(true);
            return (List<?>) field.get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("자식 엔티티를 가져올 수 없음", e);
        }
    }

    private void insertChildEntity(Object childEntity, Object parentEntity) {
        EntityColumns childEntityColumns = EntityColumns.from(childEntity.getClass());
        EntityTable childEntityTable = EntityTable.from(childEntity.getClass());
        String insertQuery = insertQueryBuilder.getInsertQuery(childEntityTable, childEntityColumns, childEntity, parentEntity);
        jdbcTemplate.execute(insertQuery);
    }


    public void delete(Object entity) {
        String deleteQuery = deleteQueryBuilder.delete(entityTable, entityColumns, getIdValue(entity));
        jdbcTemplate.execute(deleteQuery);
    }

    public Long getIdValue(Object entity) {
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
        return null;
    }

    public void setIdValue(Object entity, Long idValue) {
        Class<?> clazz = entity.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                field.setAccessible(true);
                try {
                    field.set(entity, idValue);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("ID 값을 설정하는 중 오류 발생", e);
                }
            }
        }
    }

}
