package game.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Single point of access to the MySQL connection.
 *
 * Reads DB_URL / DB_USER / DB_PASSWORD from the environment so the same
 * build can run locally, in Docker, or on a server without code changes.
 * Falls back to a local default if the env vars aren't set.
 */
public class DatabaseConnection {

    private static final String URL = getEnvOrDefault(
            "DB_URL", "jdbc:mysql://localhost:3306/game_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
    private static final String USER = getEnvOrDefault("DB_USER", "root");
    private static final String PASSWORD = getEnvOrDefault("DB_PASSWORD", "rootpass");

    private static Connection connection;

    private DatabaseConnection() {
        // utility class — no instances
    }

    private static String getEnvOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? fallback : value;
    }

    public static synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed() || !isConnectionValid()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new SQLException("MySQL JDBC driver not found on classpath. " +
                        "Add mysql-connector-j-<version>.jar to your classpath.", e);
            }
            try {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
            } catch (SQLException e) {
                if ("rootpass".equals(PASSWORD) && e.getMessage() != null && e.getMessage().contains("Access denied")) {
                    connection = DriverManager.getConnection(URL, USER, "");
                } else {
                    throw e;
                }
            }
        }
        return connection;
    }

    private static boolean isConnectionValid() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    public static synchronized void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        } finally {
            connection = null;
        }
    }
}
