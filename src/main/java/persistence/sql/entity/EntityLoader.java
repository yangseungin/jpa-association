package persistence.sql.entity;

import jakarta.persistence.OneToMany;
import jdbc.JdbcTemplate;
import persistence.sql.dml.SelectQueryBuilder;
import persistence.sql.domain.OrderItem;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.Connection;
import java.util.Arrays;
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
        EntityTable entityTable = EntityTable.from(clazz);
        EntityColumns entityColumns = EntityColumns.from(clazz);

        Field oneToManyField = Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(OneToMany.class))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("OneToMany를 찾지 못하였음"));

        Class<?> joinEntityClass = (Class<?>) ((ParameterizedType) oneToManyField.getGenericType()).getActualTypeArguments()[0];
        EntityTable joinTable = EntityTable.from(joinEntityClass);
        EntityColumns joinColumns = EntityColumns.from(joinEntityClass);

        String selectWithJoinQuery = selectQueryBuilder.findAllWithJoin(entityTable, entityColumns, joinTable, joinColumns);

        return jdbcTemplate.query(selectWithJoinQuery, new EntityRowMapper<>(clazz));
    }
}
