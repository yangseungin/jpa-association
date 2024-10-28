package persistence.sql;

public enum SqlType {
    VARCHAR,
    INTEGER,
    BIGINT,
    ARRAY
    ;

    public static SqlType from(String type) {
        return switch (type) {
            case "Long" -> BIGINT;
            case "String" -> VARCHAR;
            case "Integer" -> INTEGER;
            case "List" -> ARRAY;
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }
}
