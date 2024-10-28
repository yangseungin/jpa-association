package persistence.sql;

import persistence.sql.definition.ColumnDefinition;

public interface Queryable {

    void applyToCreateTableQuery(StringBuilder query, Dialect dialect);

    boolean hasValue(Object entity);

    String getValueWithQuoted(Object entity);

    Object getValue(Object entity);

    String getColumnName();

    String getDeclaredName();

    ColumnDefinition getColumnDefinition();
}
