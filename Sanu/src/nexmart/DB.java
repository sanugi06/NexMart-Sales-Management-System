package nexmart;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DB {

    private static final String URL  = "jdbc:mysql://localhost:3306/nexmart?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "COOL2006#";

    private static Connection connection = null;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new SQLException("MySQL JDBC Driver not found.", e);
            }
            connection = DriverManager.getConnection(URL, USER, PASS);
        }
        return connection;
    }

    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException ignored) {}
    }
}
