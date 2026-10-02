package tech.kood.rental;

import org.junit.jupiter.api.Test;
import tech.kood.rental.infrastructure.DatabaseConnection;
import tech.kood.rental.repository.exception.CannotOpenDatabaseException;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {
    @Test
    void defaultMainUsesConfiguredDatabasePath() throws Exception {
        Path databasePath = Files.createTempFile("rental-main-default-", ".db");
        String previousDatabaseProperty = System.getProperty("rental.database");
        var originalIn = System.in;
        System.setProperty("rental.database", databasePath.toString());
        System.setIn(new java.io.ByteArrayInputStream("3\n".getBytes()));
        try {
            new Main();
            Main.main(new String[0]);
            try (Connection connection = new DatabaseConnection(databasePath.toString()).getConnection();
                 Statement statement = connection.createStatement();
                 var result = statement.executeQuery("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'")) {
                assertTrue(result.next());
                assertEquals(3, result.getInt(1));
            }
        } finally {
            System.setIn(originalIn);
            if (previousDatabaseProperty == null) System.clearProperty("rental.database");
            else System.setProperty("rental.database", previousDatabaseProperty);
            Files.deleteIfExists(databasePath);
        }
    }

    @Test
    void startupInitializesSchemaAtRequestedPath() throws Exception {
        Path databasePath = Files.createTempFile("rental-main-", ".db");
        var originalIn = System.in;
        System.setIn(new java.io.ByteArrayInputStream("3\n".getBytes()));
        try {
            Main.main(new String[]{databasePath.toString()});
            try (Connection connection = new DatabaseConnection(databasePath.toString()).getConnection();
                 Statement statement = connection.createStatement();
                 var result = statement.executeQuery("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'")) {
                assertTrue(result.next());
                assertEquals(3, result.getInt(1));
            }
        } finally {
            System.setIn(originalIn);
            Files.deleteIfExists(databasePath);
        }
    }

    @Test
    void startupReportsDatabaseInitializationFailure() throws Exception {
        Path parent = Files.createTempDirectory("rental-main-invalid-");
        Path invalidPath = parent.resolve("missing").resolve("app.db");
        var originalIn = System.in;
        var originalErr = System.err;
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        System.setIn(new java.io.ByteArrayInputStream("3\n".getBytes()));
        System.setErr(new PrintStream(errors));
        try {
            Main.main(new String[]{invalidPath.toString()});
        } finally {
            System.setIn(originalIn);
            System.setErr(originalErr);
            Files.deleteIfExists(parent);
        }

        assertTrue(errors.toString().contains("Failed to initialize database:"));
    }

    @Test
    void startupHandlesMissingSchemaResource() throws Exception {
        Path databasePath = Files.createTempFile("rental-main-no-schema-", ".db");
        try {
            Main.initializeDatabase(new DatabaseConnection(databasePath.toString()), (InputStream) null);
            try (Connection connection = new DatabaseConnection(databasePath.toString()).getConnection();
                 Statement statement = connection.createStatement();
                 var result = statement.executeQuery("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'")) {
                assertTrue(result.next());
                assertEquals(0, result.getInt(1));
            }
        } finally {
            Files.deleteIfExists(databasePath);
        }
    }

    @Test
    void startupReportsSchemaExecutionFailure() throws Exception {
        Path databasePath = Files.createTempFile("rental-main-bad-schema-", ".db");
        var originalErr = System.err;
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errors));
        try {
            boolean initialized = Main.initializeDatabase(
                    new DatabaseConnection(databasePath.toString()),
                    new ByteArrayInputStream("INVALID SQL;".getBytes()) {
                        @Override
                        public void close() throws IOException {
                            super.close();
                            throw new IOException("failed to close schema stream");
                        }
                    });
            assertFalse(initialized);
        } finally {
            System.setErr(originalErr);
            Files.deleteIfExists(databasePath);
        }

        assertTrue(errors.toString().contains("Failed to initialize database:"));
    }

    @Test
    void invalidDatabasePathThrowsConnectionException() {
        Path missingParent = Path.of("target", "missing-parent", "app.db");

        assertThrows(CannotOpenDatabaseException.class,
                () -> new DatabaseConnection(missingParent.toString()).getConnection());
    }
}