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
            return DriverManager.getConnection(dbUrl);
        } catch (Exception e) {
            throw new CannotOpenDatabaseException("Failed to establish SQLite database connection.", e);
        }
    }
}
