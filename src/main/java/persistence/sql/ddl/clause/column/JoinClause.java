package persistence.sql.ddl.clause.column;

import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import persistence.sql.ddl.clause.table.TableClause;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class JoinClause {
    public static final String JOIN_QUERY = " inner join %s on %s.%s = %s.%s";

    private final TableClause childClause;
    private final String referenceKeyName;
    public JoinClause(Class<?> clazz, String referenceKeyName) {
        this.childClause = new TableClause(clazz);
        this.referenceKeyName = referenceKeyName;
    }

    public static boolean hasOneToMany(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .anyMatch(x -> x.isAnnotationPresent(OneToMany.class));
    }

    public static Field findChildEntityField(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(x -> x.isAnnotationPresent(OneToMany.class))
                .findFirst().get();
    }
    public static Class<?> childEntityClass(Class<?> clazz) {
        Optional<Field> childEntity = Arrays.stream(clazz.getDeclaredFields())
                .filter(x -> x.isAnnotationPresent(OneToMany.class))
                .findFirst();

        if (childEntity.isEmpty() || isFetchTypeLAZY(childEntity.get())) {
            return null;
        }
        return (Class<?>) ((ParameterizedType) childEntity.get().getGenericType()).getActualTypeArguments()[0];
    }

    public static <T> List<T> childEntityClasses(Object entity) {
        Class<?> clazz = entity.getClass();

        List<Field> fields = Arrays.stream(clazz.getDeclaredFields())
                .filter(x -> x.isAnnotationPresent(OneToMany.class))
                .filter(x -> !isFetchTypeLAZY(x))
                .toList();

        List<T> childEntities = new ArrayList<>();

        for (Field field : fields) {
            getChildEntities(entity, field, childEntities);
        }

        return childEntities;
    }

    private static <T> void getChildEntities(Object entity, Field field, List<T> childEntities) {
        try {
            field.setAccessible(true);
            Object value = field.get(entity);
            childEntities.addAll((List<T>) value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("OneToMany 관계에서 many 관계의 엔티티 추출에 실패했습니다.");
        }
    }

    public static JoinClause newOne(Class<?> clazz) {
        Optional<Field> childEntity = Arrays.stream(clazz.getDeclaredFields())
                .filter(x -> x.isAnnotationPresent(OneToMany.class))
                .findFirst();

        if (childEntity.isEmpty() || isFetchTypeLAZY(childEntity.get())) {
            return null;
        }
        Class<?> childEntityClass = (Class<?>) ((ParameterizedType) childEntity.get().getGenericType()).getActualTypeArguments()[0];
        String referenceKeyName = childEntity.get().getAnnotation(JoinColumn.class).name();
        return new JoinClause(childEntityClass, referenceKeyName);
    }

    private static boolean isFetchTypeLAZY(Field childEntity) {
        return FetchType.LAZY == childEntity.getAnnotation(OneToMany.class).fetch();
    }

    public String getJoinQuery(String parentEntity, String referenceKey) {
        return String.format(JOIN_QUERY,
                childClause.name(), parentEntity, referenceKey, childClause.name(), referenceKeyName);
    }

    public TableClause getChildClause() {
        return childClause;
    }
}
