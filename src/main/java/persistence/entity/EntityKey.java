package persistence.entity;

import java.io.Serializable;
import java.util.Objects;

public record EntityKey(Serializable id, Class<?> entityClass) {
    public EntityKey(Serializable id, Class<?> entityClass) {
        this.id = Objects.requireNonNull(id);
        this.entityClass = entityClass;
    }
}
