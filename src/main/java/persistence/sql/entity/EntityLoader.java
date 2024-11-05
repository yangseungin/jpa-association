package persistence.sql.entity;

import jdbc.JdbcTemplate;
import persistence.sql.Metadata;
import persistence.sql.dml.SelectQueryBuilder;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.List;

public class EntityLoader {
    private final JdbcTemplate jdbcTemplate;
    private final SelectQueryBuilder selectQueryBuilder;

    public EntityLoader(Connection connection) {
        this.jdbcTemplate = new JdbcTemplate(connection);
        this.selectQueryBuilder = new SelectQueryBuilder();
    }

    public <T> T loadEntity(Class<T> clazz, Long id) {
        EntityTable entityTable = EntityTable.from(clazz);
        EntityColumns entityColumns = EntityColumns.from(clazz);

        String selectQuery = selectQueryBuilder.findById(entityTable, entityColumns, id);
        return jdbcTemplate.queryForObject(selectQuery, new EntityRowMapper<>(clazz));
    }

    public <T> List<T> loadEntitiesWithJoin(Class<T> clazz) {
        Metadata mainMetadata = new Metadata(clazz);

        Field oneToManyField = mainMetadata.getOneToManyField();

        Class<?> joinEntityClass = mainMetadata.getJoinEntityClass(oneToManyField);
        Metadata joinMetadata = new Metadata(joinEntityClass);

        String selectWithJoinQuery = selectQueryBuilder.findAllWithJoin(
                mainMetadata.getEntityTable(), mainMetadata.getEntityColumns(),
                joinMetadata.getEntityTable(), joinMetadata.getEntityColumns()
        );

        return jdbcTemplate.query(selectWithJoinQuery, new EntityRowMapper<>(clazz));
    }
}
