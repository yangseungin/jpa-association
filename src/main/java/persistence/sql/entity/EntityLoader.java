package persistence.sql.entity;

import jdbc.JdbcTemplate;
import persistence.sql.Metadata;
import persistence.sql.dml.SelectQueryBuilder;

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

        List<Class<?>> joinEntityClasses = mainMetadata.getJoinEntityClasses();

        String selectWithJoinQuery = selectQueryBuilder.findAllWithJoin(
                mainMetadata.getEntityTable(), mainMetadata.getEntityColumns(),
                mainMetadata.getEntityTable(), mainMetadata.getEntityColumns()
        );

        for (Class<?> joinEntityClass : joinEntityClasses) {
            Metadata joinMetadata = new Metadata(joinEntityClass);
            selectWithJoinQuery = selectQueryBuilder.findAllWithJoin(
                    mainMetadata.getEntityTable(), mainMetadata.getEntityColumns(),
                    joinMetadata.getEntityTable(), joinMetadata.getEntityColumns()
            );

        }

        return jdbcTemplate.query(selectWithJoinQuery, new EntityRowMapper<>(clazz));
    }
}
