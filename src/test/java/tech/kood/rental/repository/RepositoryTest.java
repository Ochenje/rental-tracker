package tech.kood.rental.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.kood.rental.TestDatabase;
import tech.kood.rental.domain.Item;
import tech.kood.rental.domain.Rental;
import tech.kood.rental.domain.User;
import tech.kood.rental.repository.exception.CheckConstraintViolationException;
import tech.kood.rental.repository.exception.ConstraintViolationException;
import tech.kood.rental.repository.exception.DatabaseException;
import tech.kood.rental.repository.exception.ForeignKeyViolationException;
import tech.kood.rental.repository.exception.MappingException;
import tech.kood.rental.repository.exception.NotFoundException;
import tech.kood.rental.repository.exception.NotNullViolationException;
import tech.kood.rental.repository.exception.UniqueConstraintViolationException;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryTest {
    private TestDatabase testDatabase;

    @BeforeEach
    void setUp() throws Exception {
        testDatabase = TestDatabase.create();
    }

    @AfterEach
    void tearDown() throws Exception {
        testDatabase.close();
    }

    @Test
    void insertUserPersistsAndSupportsIdAndUsernameQueries() throws Exception {
        testDatabase.users.insert("alice");

        assertEquals(1, count("SELECT COUNT(*) FROM users WHERE username = 'alice'"));
        User user = testDatabase.users.findByUsername("alice");
        assertNotNull(user);
        assertEquals("alice", user.username);
        assertEquals(user.id, testDatabase.users.findById(user.id).id);
        assertNull(testDatabase.users.findById(-1));
        assertNull(testDatabase.users.findByUsername("missing"));
    }

    @Test
    void insertItemPersistsAndSupportsStatusOwnerAndIdQueries() throws Exception {
        int ownerId = createUser("owner");
        testDatabase.items.insert(ownerId, "Hammer", "Steel hammer", 2.5, "available");

        assertEquals(1, count("SELECT COUNT(*) FROM listed_items WHERE item_name = 'Hammer' AND status = 'available'"));
        Item item = testDatabase.items.findById(1);
        assertNotNull(item);
        assertEquals("Hammer", item.itemName);
        assertEquals(1, testDatabase.items.findAllAvailable().size());
        assertEquals(1, testDatabase.items.findByOwnerId(ownerId).size());
        assertNull(testDatabase.items.findById(-1));
        assertTrue(testDatabase.items.findByOwnerId(-1).isEmpty());
    }

    @Test
    void insertRentalPersistsAndSupportsIdAndFilterQueries() throws Exception {
        int ownerId = createUser("owner");
        int renterId = createUser("renter");
        testDatabase.items.insert(ownerId, "Camera", "Digital camera", 10, "rented");
        testDatabase.rentals.insert(1, renterId, "2026-03-01", "2026-03-04");

        assertEquals(1, count("SELECT COUNT(*) FROM rentals WHERE item_id = 1 AND status = 'active'"));
        Rental rental = testDatabase.rentals.findById(1);
        assertNotNull(rental);
        assertEquals("2026-03-04", rental.endTime);
        assertEquals(rental.rentalId, testDatabase.rentals.findActiveByItemId(1).rentalId);
        assertEquals(1, testDatabase.rentals.findByRenterId(renterId).size());
        assertNull(testDatabase.rentals.findById(-1));
        assertNull(testDatabase.rentals.findActiveByItemId(-1));
        assertTrue(testDatabase.rentals.findByRenterId(-1).isEmpty());
    }

    @Test
    void updateItemStatusPersistsAndRejectsInvalidStatus() throws Exception {
        int ownerId = createUser("owner");
        testDatabase.items.insert(ownerId, "Drill", "Cordless drill", 5, "available");
        testDatabase.items.updateStatus(1, "unlisted");

        assertEquals("unlisted", text("SELECT status FROM listed_items WHERE item_id = 1"));
        assertThrows(CheckConstraintViolationException.class, () -> testDatabase.items.updateStatus(1, "unknown"));
        assertThrows(NotFoundException.class, () -> testDatabase.items.updateStatus(99, "available"));
    }

    @Test
    void closeRentalPersistsReturnedTimeAndStatus() throws Exception {
        int ownerId = createUser("owner");
        int renterId = createUser("renter");
        testDatabase.items.insert(ownerId, "Tent", "Camping tent", 4, "rented");
        testDatabase.rentals.insert(1, renterId, "2026-03-01", "2026-03-05");
        testDatabase.rentals.closeRental(1, "2026-03-04");

        assertEquals("closed", text("SELECT status FROM rentals WHERE rental_id = 1"));
        assertEquals("2026-03-04", text("SELECT returned_at FROM rentals WHERE rental_id = 1"));
        assertThrows(NotFoundException.class, () -> testDatabase.rentals.closeRental(99, "2026-03-04"));
    }

    @Test
    void duplicateUsernameThrowsUniqueConstraintException() {
        testDatabase.users.insert("same-name");

        assertThrows(UniqueConstraintViolationException.class, () -> testDatabase.users.insert("same-name"));
    }

    @Test
    void rentalForMissingItemThrowsForeignKeyException() {
        int renterId = createUser("renter");

        assertThrows(ForeignKeyViolationException.class,
                () -> testDatabase.rentals.insert(404, renterId, "2026-03-01", "2026-03-02"));
    }

    @Test
    void itemMissingRequiredNameThrowsNotNullException() {
        int ownerId = createUser("owner");

        assertThrows(NotNullViolationException.class,
                () -> testDatabase.items.insert(ownerId, null, "No name", 1, "available"));
    }

    @Test
    void invalidItemStatusThrowsCheckConstraintException() {
        int ownerId = createUser("owner");

        assertThrows(CheckConstraintViolationException.class,
                () -> testDatabase.items.insert(ownerId, "Saw", "Hand saw", 1, "listed"));
    }

    @Test
    void databaseConstraintClassifierHasGenericFallback() {
        ConstraintViolationException exception = ConstraintViolationException.from(new SQLException("constraint failed"));

        assertEquals(ConstraintViolationException.class, exception.getClass());
        assertEquals(ConstraintViolationException.class,
                ConstraintViolationException.from(new SQLException((String) null)).getClass());
        assertTrue(ConstraintViolationException.isConstraintViolation(new SQLException("constraint", "", 1)));
        assertFalse(ConstraintViolationException.isConstraintViolation(new SQLException("ordinary error", "", 1)));
        assertTrue(ConstraintViolationException.isConstraintViolation(new SQLException("ordinary error", "", 19)));
        assertFalse(ConstraintViolationException.isConstraintViolation(new SQLException((String) null, "", 1)));
    }

    @Test
    void repositorySqlFailuresAreWrappedAsDatabaseExceptions() throws Exception {
        try (Connection connection = testDatabase.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE rentals");
            statement.execute("DROP TABLE listed_items");
            statement.execute("DROP TABLE users");
        }

        assertThrows(DatabaseException.class, () -> testDatabase.users.insert("alice"));
        assertThrows(DatabaseException.class, () -> testDatabase.users.findById(1));
        assertThrows(DatabaseException.class, () -> testDatabase.users.findByUsername("alice"));
        assertThrows(DatabaseException.class, () -> testDatabase.items.insert(1, "Saw", "Hand saw", 1, "available"));
        assertThrows(DatabaseException.class, () -> testDatabase.items.findAllAvailable());
        assertThrows(DatabaseException.class, () -> testDatabase.items.findById(1));
        assertThrows(DatabaseException.class, () -> testDatabase.items.findByOwnerId(1));
        assertThrows(DatabaseException.class, () -> testDatabase.items.updateStatus(1, "available"));
        assertThrows(DatabaseException.class, () -> testDatabase.rentals.insert(1, 1, "start", "end"));
        assertThrows(DatabaseException.class, () -> testDatabase.rentals.findById(1));
        assertThrows(DatabaseException.class, () -> testDatabase.rentals.findByRenterId(1));
        assertThrows(DatabaseException.class, () -> testDatabase.rentals.findActiveByItemId(1));
        assertThrows(DatabaseException.class, () -> testDatabase.rentals.closeRental(1, "returned"));
    }

    @Test
    void closeRentalMapsSqlConstraintFailure() throws Exception {
        int ownerId = createUser("owner");
        int renterId = createUser("renter");
        testDatabase.items.insert(ownerId, "Tent", "Camping tent", 4, "rented");
        testDatabase.rentals.insert(1, renterId, "2026-03-01", "2026-03-05");
        try (Connection connection = testDatabase.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TRIGGER reject_rental_close BEFORE UPDATE ON rentals " +
                    "BEGIN SELECT RAISE(ABORT, 'CHECK constraint failed: close rejected'); END");
        }

        assertThrows(CheckConstraintViolationException.class,
                () -> testDatabase.rentals.closeRental(1, "2026-03-04"));
    }

    @Test
    void mappingFailureIsReportedAsMappingException() throws Exception {
        try (Connection connection = testDatabase.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE listed_items");
            statement.execute("CREATE TABLE listed_items (item_id INTEGER PRIMARY KEY)");
            statement.execute("INSERT INTO listed_items VALUES (1)");
        }

        assertThrows(MappingException.class, () -> testDatabase.items.findById(1));
    }

    @Test
    void databaseFailureIsReportedAsDatabaseException() throws Exception {
        try (Connection connection = testDatabase.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE users");
        }

        assertThrows(DatabaseException.class, () -> testDatabase.users.findById(1));
    }

    private int createUser(String username) {
        testDatabase.users.insert(username);
        return testDatabase.users.findByUsername(username).id;
    }

    private int count(String sql) throws Exception {
        try (Connection connection = testDatabase.getConnection(); Statement statement = connection.createStatement();
             var result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }

    private String text(String sql) throws Exception {
        try (Connection connection = testDatabase.getConnection(); Statement statement = connection.createStatement();
             var result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getString(1);
        }
    }
}
