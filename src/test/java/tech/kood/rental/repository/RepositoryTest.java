package tech.kood.rental.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.kood.rental.domain.Item;
import tech.kood.rental.infrastructure.DatabaseConnection;
import tech.kood.rental.repository.exception.ConstraintViolationException;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RepositoryTest {
    private File testDb;
    private DatabaseConnection db;
    private ItemRepository repo;

    @BeforeEach
    public void setup() throws Exception {
        testDb = File.createTempFile("repo_test", ".db");
        db = new DatabaseConnection(testDb.getAbsolutePath());
        repo = new ItemRepository(db);

        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE listed_items (item_id INTEGER PRIMARY KEY AUTOINCREMENT, owner_id INTEGER, item_name TEXT NOT NULL, description TEXT, cost_per_day REAL, status TEXT CHECK(status IN ('available','rented','unlisted')), created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
        }
    }

    @AfterEach
    public void clean() throws Exception {
        Files.deleteIfExists(testDb.toPath());
    }

    @Test
    public void testInsertAndRetrievalPipeline() {
        repo.insert(10, "Hammer", "Steel hammer", 2.50, "available");
        List<Item> listings = repo.findAllAvailable();
        assertEquals(1, listings.size());
        assertEquals("Hammer", listings.get(0).itemName);
    }

    @Test
    public void testCheckConstraintBlocksBadData() {
        assertThrows(ConstraintViolationException.class, () -> {
            repo.insert(10, "Hammer", "Steel hammer", 2.50, "INVALID_STATE_FLAG");
        });
    }
}
