package persistence.sql.ddl.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import persistence.sql.Dialect;
import persistence.sql.definition.ColumnDefinition;
import persistence.sql.definition.TableDefinition;
import persistence.sql.definition.TableId;

public class CreateTableQueryBuilder {
    private final StringBuilder query;
    private final Logger logger = LoggerFactory.getLogger(CreateTableQueryBuilder.class);

    public CreateTableQueryBuilder(
            Dialect dialect,
            Class<?> entityClass,
            Class<?> parentClass
    ) {
        this.query = new StringBuilder();

        TableDefinition tableDefinition = new TableDefinition(entityClass);

        query.append("CREATE TABLE ").append(tableDefinition.getTableName());
        query.append(" (");

        tableDefinition.withIdColumns().forEach(column -> column.applyToCreateTableQuery(query, dialect));
        if (parentClass != null) {
            TableDefinition parentTableDefinition = new TableDefinition(parentClass);
            parentTableDefinition.getAssociations().forEach(association -> {
                if (association.getAssociatedEntityClass().equals(entityClass)) {
                    if (association.getJoinColumnName() != null) {
                        query.append(association.getJoinColumnName() + " " + dialect.translateType(
                                getColumnDefinition(parentTableDefinition, association.getJoinColumnName())
                        ) + ", ");
                    }
                }
            });

        }

        definePrimaryKey(tableDefinition.getTableId(), query);

        query.append(");");
    }

    private ColumnDefinition getColumnDefinition(TableDefinition parentTableDefinition, String joinColumnName) {
        return parentTableDefinition.getColumn(joinColumnName).getColumnDefinition();
    }

    public String build() {
        final String sql = query.toString();
        logger.info("Generated Create Table SQL: {}", sql);
        return sql;
    }

    private void definePrimaryKey(TableId pk, StringBuilder query) {
        query.append("PRIMARY KEY (").append(pk.getColumnName()).append(")");
    }
}
