package persistence.sql.entity;

public interface PersistenceContext {
    <T> T getEntity(Class<T> clazz, Long id);

    void addEntity(Object entity, Long id);

    void removeEntity(Class<?> clazz, Long id);

    boolean containsEntity(EntityKey entityKey);

    Object getDatabaseSnapshot(Long id, Object entity);

    void addSnapshot(Long id, Object entity);

    boolean isDirty(Long id, Object currentEntity);

    void addEntry(Object entity, EntityEntry entityEntry);

    void removePersistenceContext(EntityKey entityKey, Object entity);
}
