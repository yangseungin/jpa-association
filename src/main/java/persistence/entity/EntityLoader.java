package persistence.entity;

import jdbc.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import persistence.sql.definition.TableDefinition;
import persistence.sql.dml.query.SelectQueryBuilder;

public class EntityLoader {
    private final JdbcTemplate jdbcTemplate;
    private final Logger logger = LoggerFactory.getLogger(EntityLoader.class);

    public EntityLoader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public <T> T loadEntity(Class<T> entityClass, EntityKey entityKey) {
        final SelectQueryBuilder queryBuilder = new SelectQueryBuilder(entityKey.entityClass());
        final TableDefinition tableDefinition = new TableDefinition(entityKey.entityClass());

        tableDefinition.getAssociations().forEach(association -> {
            if (association.isFetchEager()) {
                queryBuilder.join(association);
            }
        });

        final String query = queryBuilder.build(entityKey.id());
        logger.info("Executing custom select query: {}", query);

        final Object queried = jdbcTemplate.queryForObject(query,
                new EntityRowMapper<>(entityKey.entityClass())
        );

        return entityClass.cast(queried);
    }
}
