package tech.kood.rental;

import tech.kood.rental.infrastructure.DatabaseConnection;
import tech.kood.rental.repository.ItemRepository;
import tech.kood.rental.repository.RentalRepository;
import tech.kood.rental.repository.UserRepository;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;

public final class TestDatabase implements AutoCloseable {
    private final Path path;
    public final DatabaseConnection db;
    public final UserRepository users;
    public final ItemRepository items;
    public final RentalRepository rentals;

    private TestDatabase(Path path) throws Exception {
        this.path = path;
        db = new DatabaseConnection(path.toString());
        users = new UserRepository(db);
        items = new ItemRepository(db);
        rentals = new RentalRepository(db);
        initializeSchema();
    }

    public static TestDatabase create() throws Exception {
        return new TestDatabase(Files.createTempFile("rental-test-", ".db"));
    }

    public Path getPath() {
        return path;
    }

    public Connection getConnection() {
        return db.getConnection();
    }

    private void initializeSchema() throws Exception {
        try (InputStream input = TestDatabase.class.getResourceAsStream("/schema.sql")) {
            String schema = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            try (Connection connection = db.getConnection(); Statement statement = connection.createStatement()) {
                for (String sql : schema.split(";")) {
                    if (!sql.isBlank()) statement.execute(sql);
                }
            }
        }
    }

    @Override
    public void close() throws Exception {
        Files.deleteIfExists(path);
    }
}