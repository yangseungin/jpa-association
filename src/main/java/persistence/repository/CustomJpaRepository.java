package persistence.repository;

import jdbc.JdbcTemplate;
import persistence.PrimaryKey;
import persistence.entity.manager.EntityManager;
import persistence.entity.manager.EntityManagerImpl;

import java.util.Optional;

public class CustomJpaRepository {
    private final EntityManager entityManager;
    public CustomJpaRepository(JdbcTemplate jdbcTemplate) {
        this.entityManager = new EntityManagerImpl(jdbcTemplate);
    }


    <T> T save (T entity) {
        boolean isInEntityManger = entityManager.find(entity.getClass(), new PrimaryKey(entity).getPrimaryKeyValue(entity)).isPresent();

        if (isInEntityManger) {
           return entityManager.merge(entity);
        }
        entityManager.persist(entity);
        Long id= new PrimaryKey(entity).getPrimaryKeyValue(entity);
        return (T) entityManager.find(entity.getClass(), id).get();
    }

    <T> Optional<T> find(Class<T> clazz, Long id) {
        return entityManager.find(clazz, id);
    }
}
