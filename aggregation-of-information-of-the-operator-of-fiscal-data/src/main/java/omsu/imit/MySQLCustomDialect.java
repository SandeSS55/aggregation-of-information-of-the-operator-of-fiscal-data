package omsu.imit;

import org.hibernate.dialect.MySQL5Dialect;

public class MySQLCustomDialect extends MySQL5Dialect {
    @Override
    public String getTableTypeString() {
        return " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
    }
}
