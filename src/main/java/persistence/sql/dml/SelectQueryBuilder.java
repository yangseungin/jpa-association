package persistence.sql.dml;

import jakarta.persistence.JoinColumn;
import persistence.sql.entity.EntityColumn;
import persistence.sql.entity.EntityColumns;
import persistence.sql.entity.EntityTable;

import java.util.List;
import java.util.stream.Collectors;

public class SelectQueryBuilder {

    public String findAll(EntityTable entityTable, EntityColumns entityColumns) {
        String tableName = entityTable.getTableName();
        String tableColumns = getTableColumns(entityColumns, tableName);
        return String.format("select %s FROM %s", tableColumns, tableName);
    }

    public String findById(EntityTable entityTable, EntityColumns entityColumns, Object idValue) {
        String selectQuery = findAll(entityTable, entityColumns);
        String idField = entityColumns.getIdFieldName();
        String formattedIdValue = getFormattedId(idValue);
        return String.format("%s where %s = %s", selectQuery, idField, formattedIdValue);
    }

    public String findAllWithJoin(EntityTable mainTable, EntityColumns mainColumns,
                                  EntityTable joinTable, EntityColumns joinColumns) {
        String mainTableName = mainTable.getTableName();
        String joinTableName = joinTable.getTableName();
        String mainTableColumns = getTableColumns(mainColumns, mainTableName);
        String joinTableColumns = getTableColumns(joinColumns, joinTableName);

        String mainJoinColumn = mainColumns.getIdFieldName();
        String joinJoinColumn = getJoinColumnName(mainColumns);

        return String.format("SELECT %s, %s FROM %s LEFT JOIN %s ON %s.%s = %s.%s",
                mainTableColumns, joinTableColumns,
                mainTableName, joinTableName,
                mainTableName, mainJoinColumn, joinTableName, joinJoinColumn);
    }

    private String getJoinColumnName(EntityColumns mainColumns) {
        List<EntityColumn> oneToManyColumns = mainColumns.getOneToManyColumns();

        if (oneToManyColumns.isEmpty()) {
            return null;
        }

        return oneToManyColumns.stream()
                .map(oneToManyColumn -> {
                    JoinColumn joinColumn = oneToManyColumn.getField().getAnnotation(JoinColumn.class);
                    if (joinColumn != null) {
                        return joinColumn.name();
                    }
                    return oneToManyColumn.getField().getName() + "_id";
                })
                .collect(Collectors.joining(", "));
    }

    private String getTableColumns(EntityColumns entityColumns, String tableAlias) {
        return entityColumns.getColumns()
                .stream()
                .filter(entityColumn -> !entityColumn.isTransient())
                .filter(entityColumn -> !entityColumn.isOneToMany())
                .map(entityColumn -> tableAlias + "." + entityColumn.getColumnName())
                .collect(Collectors.joining(", "));
    }

    private String getFormattedId(Object idValue) {
        if (idValue instanceof String) {
            return String.format(("'%s'"), idValue);
        }
        return idValue.toString();
    }
}
