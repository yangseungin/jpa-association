package persistence.entity;

import common.ReflectionFieldAccessUtils;
import jdbc.JdbcTemplate;
import persistence.sql.definition.TableDefinition;

import java.io.Serializable;
import java.util.function.Supplier;

public class EntityManagerImpl implements EntityManager {
    private final PersistenceContext persistenceContext;
    private final EntityPersister entityPersister;
    private final EntityLoader entityLoader;

    public EntityManagerImpl(JdbcTemplate jdbcTemplate,
                             PersistenceContext persistenceContext) {

        this.persistenceContext = persistenceContext;
        this.entityPersister = new EntityPersister(jdbcTemplate);
        this.entityLoader = new EntityLoader(jdbcTemplate);
    }

    @Override
    public <T> T find(Class<T> clazz, Object id) {
        final EntityKey entityKey = new EntityKey((Long) id, clazz);
        final EntityEntry entityEntry = getEntityEntryOrDefault(entityKey, () -> EntityEntry.loading((Serializable) id));

        if (entityEntry.isManaged()) {
            return clazz.cast(persistenceContext.getEntity(entityKey));
        }

        if (entityEntry.isNotReadable()) {
            throw new IllegalArgumentException("Entity is not managed: " + clazz.getSimpleName());
        }

        final T loaded = entityLoader.loadEntity(clazz, entityKey);
        addEntityInContext(entityKey, loaded);
        addManagedEntityEntry(entityKey, entityEntry);
        return loaded;
    }

    private EntityEntry getEntityEntryOrDefault(EntityKey entityKey, Supplier<EntityEntry> defaultEntrySupplier) {
        final EntityEntry entityEntry = persistenceContext.getEntityEntry(entityKey);
        if (entityEntry == null) {
            return defaultEntrySupplier.get();
        }

        return entityEntry;
    }

    @Override
    public void persist(Object entity) {
        if (entityPersister.hasId(entity)) {
            final EntityEntry entityEntry = persistenceContext.getEntityEntry(
                    new EntityKey(entityPersister.getEntityId(entity), entity.getClass())
            );

            if (entityEntry == null) {
                throw new IllegalArgumentException("No Entity Entry with id: " + entityPersister.getEntityId(entity));
            }

            if (entityEntry.isManaged()) {
                return;
            }

            throw new IllegalArgumentException("Entity already persisted");
        }

        final EntityEntry entityEntry = EntityEntry.inSaving();

        entityPersister.insert(entity);
        final EntityKey entityKey = new EntityKey(entityPersister.getEntityId(entity), entity.getClass());
        addEntityInContext(entityKey, entity);
        addManagedEntityEntry(entityKey, entityEntry);

        manageChildEntity(entity);
    }

    @Override
    public void remove(Object entity) {
        final EntityKey entityKey = new EntityKey(entityPersister.getEntityId(entity), entity.getClass());
        final EntityEntry entityEntry = persistenceContext.getEntityEntry(entityKey);
        checkManagedEntity(entity, entityEntry);

        entityEntry.updateStatus(Status.DELETED);
        entityPersister.delete(entity);
        persistenceContext.removeEntity(entityKey);
    }

    @Override
    public <T> T merge(T entity) {
        final EntityKey entityKey = new EntityKey(entityPersister.getEntityId(entity), entity.getClass());
        final EntityEntry entityEntry = persistenceContext.getEntityEntry(entityKey);
        checkManagedEntity(entity, entityEntry);

        final EntitySnapshot entitySnapshot = persistenceContext.getDatabaseSnapshot(entityKey);
        if (entitySnapshot.hasDirtyColumns(entity)) {
            entityPersister.update(entity);
        }

        addEntityInContext(entityKey, entity);
        addManagedEntityEntry(entityKey, entityEntry);
        return entity;
    }

    @Override
    public void clear() {
        persistenceContext.clear();
    }

    private void checkManagedEntity(Object entity, EntityEntry entityEntry) {
        if (entityEntry == null) {
            throw new IllegalStateException("Can not find entity in persistence context: "
                    + entity.getClass().getSimpleName());
        }

        if (!entityEntry.isManaged()) {
            throw new IllegalArgumentException("Detached entity can not be merged: "
                    + entity.getClass().getSimpleName());
        }
    }

    private void addEntityInContext(EntityKey entityKey, Object entity) {
        persistenceContext.addEntity(entityKey, entity);
        persistenceContext.addDatabaseSnapshot(entityKey, entity);
    }

    private void addManagedEntityEntry(EntityKey entityKey, EntityEntry entityEntry) {
        entityEntry.updateStatus(Status.MANAGED);
        persistenceContext.addEntry(entityKey, entityEntry);
    }

    private void manageChildEntity(Object entity) {
        TableDefinition tableDefinition = entityPersister.getTableDefinition(entity);
        tableDefinition.getAssociations().forEach(association -> {
            final Class<?> associationClass = association.getAssociatedEntityClass();
            try {
                Object associationField = ReflectionFieldAccessUtils.accessAndGet(entity, tableDefinition.getEntityClass().getDeclaredField(association.getFieldName()));
                if (associationField == null) {
                    return;
                }

                if (associationField instanceof Iterable<?> iterable) {
                    iterable.forEach(childEntity -> {
                        if (childEntity != null && entityPersister.hasId(childEntity)) {
                            final EntityKey entityKey = new EntityKey(entityPersister.getEntityId(childEntity), associationClass);
                            addEntityInContext(entityKey, childEntity);
                            addManagedEntityEntry(entityKey, new EntityEntry(Status.MANAGED, entityKey.id()));
                        }
                    });
                }
            } catch (NoSuchFieldException e) {
                // logging
                throw new RuntimeException(e);
            }
        });
    }
}
