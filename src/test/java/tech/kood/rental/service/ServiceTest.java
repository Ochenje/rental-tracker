package tech.kood.rental.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.kood.rental.domain.Item;
import tech.kood.rental.infrastructure.DatabaseConnection;
import tech.kood.rental.repository.ItemRepository;
import tech.kood.rental.repository.RentalRepository;
import tech.kood.rental.repository.UserRepository;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class ServiceTest {
    private File testDb;
    private DatabaseConnection db;
    private UserRepository userRepo;
    private ItemRepository itemRepo;
    private RentalRepository rentalRepo;
    private RentalService service;

    @BeforeEach
    public void setup() throws Exception {
        testDb = File.createTempFile("service_test", ".db");
        db = new DatabaseConnection(testDb.getAbsolutePath());
        userRepo = new UserRepository(db);
        itemRepo = new ItemRepository(db);
        rentalRepo = new RentalRepository(db);
        service = new RentalService(userRepo, itemRepo, rentalRepo);

        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
            stmt.execute("CREATE TABLE listed_items (item_id INTEGER PRIMARY KEY AUTOINCREMENT, owner_id INTEGER, item_name TEXT NOT NULL, description TEXT, cost_per_day REAL, status TEXT, created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
            stmt.execute("CREATE TABLE rentals (rental_id INTEGER PRIMARY KEY AUTOINCREMENT, item_id INTEGER, renter_id INTEGER, start_time TEXT, end_time TEXT, returned_at TEXT, status TEXT)");
        }
    }

    @AfterEach
    public void clean() throws Exception {
        Files.deleteIfExists(testDb.toPath());
    }

    @Test
    public void testPreventOwnerFromRentingOwnAsset() {
        userRepo.insert("alice");
        var user = userRepo.findByUsername("alice");
        assertNotNull(user);
        
        itemRepo.insert(user.id, "Projector", "4K Video projector", 15.00, "available");
        Item item = itemRepo.findAllAvailable().get(0);

        assertThrows(IllegalArgumentException.class, () -> {
            service.rentItem(item.itemId, user.id, "2026-03-01", "2026-03-05");
        });
    }

    @Test
    public void testExceptionOnBlankUsernames() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.registerUser("   ");
        });
    }
}
