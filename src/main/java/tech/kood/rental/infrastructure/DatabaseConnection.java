package tech.kood.rental.infrastructure;

import tech.kood.rental.repository.exception.CannotOpenDatabaseException;
import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConnection {
    private final String dbUrl;

    public DatabaseConnection(String dbPath) {
        this.dbUrl = "jdbc:sqlite:" + dbPath;
    }

    public Connection getConnection() {
        try {
            Connection connection = DriverManager.getConnection(dbUrl);
            try (var statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }
            return connection;
        } catch (Exception e) {
            throw new CannotOpenDatabaseException("Failed to establish SQLite database connection.", e);
        }
    }
}
