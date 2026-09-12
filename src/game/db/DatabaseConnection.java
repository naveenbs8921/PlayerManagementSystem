package game.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Single point of access to the MySQL connection.
 *
 * Reads DB_URL / DB_USER / DB_PASSWORD from the environment so the same
 * build can run locally, in Docker, or on a server without code changes.
 * Falls back to local defaults and alternate connection strategies if needed.
 */
public class DatabaseConnection {

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
    }

    private static final String DEFAULT_URL =
            "jdbc:mysql://127.0.0.1:3306/game_db?allowPublicKeyRetrieval=true&connectTimeout=5000&socketTimeout=10000&serverTimezone=UTC";
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
            connection = establishConnection();
        }
        return connection;
    }

    private static Connection establishConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found on classpath. " +
                    "Add mysql-connector-j-<version>.jar to your classpath.", e);
        }

        String primaryUrl = getEnvOrDefault("DB_URL", DEFAULT_URL);
        Set<String> urlsToTry = new LinkedHashSet<>();
        urlsToTry.add(primaryUrl);

        // Host fallbacks: try both 127.0.0.1 and mysql service hostname
        if (primaryUrl.contains("//mysql:")) {
            urlsToTry.add(primaryUrl.replace("//mysql:", "//127.0.0.1:"));
        } else if (primaryUrl.contains("//127.0.0.1:")) {
            urlsToTry.add(primaryUrl.replace("//127.0.0.1:", "//mysql:"));
        } else if (primaryUrl.contains("//localhost:")) {
            urlsToTry.add(primaryUrl.replace("//localhost:", "//127.0.0.1:"));
            urlsToTry.add(primaryUrl.replace("//localhost:", "//mysql:"));
        }

        // SSL variations: try with and without useSSL=false for each candidate host
        List<String> currentList = new ArrayList<>(urlsToTry);
        for (String u : currentList) {
            if (u.contains("useSSL=false")) {
                String clean = u.replace("useSSL=false&", "")
                                .replace("&useSSL=false", "")
                                .replace("?useSSL=false", "?")
                                .replace("??", "?");
                if (clean.endsWith("?")) clean = clean.substring(0, clean.length() - 1);
                urlsToTry.add(clean);
            } else {
                String withSslFalse = u.contains("?") ? u + "&useSSL=false" : u + "?useSSL=false";
                urlsToTry.add(withSslFalse);
            }
        }

        SQLException lastException = null;

        for (String targetUrl : urlsToTry) {
            System.out.println("[Arcadia DB] Attempting connection to: " + targetUrl + " (user: " + USER + ")...");
            long start = System.currentTimeMillis();
            try {
                Connection conn = DriverManager.getConnection(targetUrl, USER, PASSWORD);
                System.out.println("[Arcadia DB] Connected successfully in " + (System.currentTimeMillis() - start) + "ms!");
                return conn;
            } catch (SQLException e) {
                lastException = e;
                System.err.println("[Arcadia DB Warning] Could not connect to " + targetUrl + ": " + e.getMessage());

                if (e.getMessage() != null && e.getMessage().contains("Access denied") && !"".equals(PASSWORD)) {
                    System.out.println("[Arcadia DB] Retrying with empty password for local dev...");
                    try {
                        Connection conn = DriverManager.getConnection(targetUrl, USER, "");
                        System.out.println("[Arcadia DB] Connected successfully with empty password in " + (System.currentTimeMillis() - start) + "ms!");
                        return conn;
                    } catch (SQLException e2) {
                        lastException = e2;
                    }
                }
            }
        }

        System.err.println("[Arcadia DB Error] All connection attempts failed.");
        if (lastException != null) {
            lastException.printStackTrace();
            throw lastException;
        }
        throw new SQLException("Failed to connect to MySQL database across all endpoints.");
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
