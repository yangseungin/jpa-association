package common;

public class AliasRule {
    private AliasRule() {
    }

    public static String buildWith(String tableName, String columnName) {
        return tableName + "_" + columnName;
    }
}
