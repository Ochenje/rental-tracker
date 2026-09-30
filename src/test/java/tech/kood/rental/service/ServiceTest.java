package tech.kood.rental.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.kood.rental.TestDatabase;
import tech.kood.rental.domain.User;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class ServiceTest {
    private TestDatabase testDatabase;
    private RentalService service;

    @BeforeEach
    void setUp() throws Exception {
        testDatabase = TestDatabase.create();
        service = new RentalService(testDatabase.users, testDatabase.items, testDatabase.rentals);
    }

    @AfterEach
    void tearDown() throws Exception {
        testDatabase.close();
    }

    @Test
    void firstRegistrationCreatesUserAndNextRunReusesIt() throws Exception {
        User firstRun = service.getOrRegisterUser("owner");
        User nextRun = service.getOrRegisterUser(" owner ");

        assertEquals(firstRun.id, nextRun.id);
        assertEquals(1, count("SELECT COUNT(*) FROM users WHERE username = 'owner'"));
    }

    @Test
    void strictRegistrationPersistsNewUser() throws Exception {
        service.registerUser("registered");

        assertEquals(1, count("SELECT COUNT(*) FROM users WHERE username = 'registered'"));
    }

    @Test
    void listingCreatesAvailableItem() throws Exception {
        int ownerId = createUser("owner");
        service.listItem(ownerId, "Projector", "4K projector", 15);

        assertEquals("available", text("SELECT status FROM listed_items WHERE item_name = 'Projector'"));
        assertEquals(1, service.getAvailableItems().size());
        assertEquals(1, service.getUserInventory(ownerId).size());
    }

    @Test
    void rentalSetsItemRentedAndCalculatesEndDate() throws Exception {
        int ownerId = createUser("owner");
        testDatabase.items.insert(ownerId, "Camera", "Digital camera", 10, "available");
        int renterId = createUser("renter");
        service.rentItem(1, renterId, "2026-03-01", 5);

        assertEquals("rented", text("SELECT status FROM listed_items WHERE item_id = 1"));
        assertEquals("active", text("SELECT status FROM rentals WHERE item_id = 1"));
        assertEquals("2026-03-06", text("SELECT end_time FROM rentals WHERE item_id = 1"));
    }

    @Test
    void existingUsernameIsReusedForRental() throws Exception {
        int ownerId = createUser("owner");
        int renterId = createUser("existing-renter");
        testDatabase.items.insert(ownerId, "Tent", "Camping tent", 4, "available");
        service.rentItem(1, "existing-renter", "2026-03-01", 2);

        assertEquals(2, count("SELECT COUNT(*) FROM users"));
        assertEquals(renterId, count("SELECT renter_id FROM rentals WHERE item_id = 1"));
    }

    @Test
    void returningRentalClosesItAndMakesItemAvailable() throws Exception {
        int ownerId = createUser("owner");
        int renterId = createUser("renter");
        testDatabase.items.insert(ownerId, "Drill", "Cordless drill", 5, "available");
        service.rentItem(1, renterId, "2026-03-01", 2);
        service.returnItem(1, "2026-03-02");

        assertEquals("available", text("SELECT status FROM listed_items WHERE item_id = 1"));
        assertEquals("closed", text("SELECT status FROM rentals WHERE item_id = 1"));
        assertEquals("2026-03-02", text("SELECT returned_at FROM rentals WHERE item_id = 1"));
    }

    @Test
    void returningDelistedRentalLeavesItemUnlisted() throws Exception {
        int ownerId = createUser("owner");
        int renterId = createUser("renter");
        testDatabase.items.insert(ownerId, "Tent", "Camping tent", 4, "available");
        service.rentItem(1, renterId, "2026-03-01", 2);
        service.delistItem(1, ownerId);
        service.returnItem(1, "2026-03-02");

        assertEquals("unlisted", text("SELECT status FROM listed_items WHERE item_id = 1"));
        assertEquals("closed", text("SELECT status FROM rentals WHERE item_id = 1"));
    }

    @Test
    void alreadyRentedItemCannotBeRentedAgain() throws Exception {
        int ownerId = createUser("owner");
        int renterId = createUser("renter");
        testDatabase.items.insert(ownerId, "Lens", "Camera lens", 7, "available");
        service.rentItem(1, renterId, "2026-03-01", 2);

        assertThrows(IllegalStateException.class,
                () -> service.rentItem(1, "another-renter", "2026-03-03", 2));
        assertEquals(1, count("SELECT COUNT(*) FROM rentals"));
    }

    @Test
    void delistedItemCannotBeRented() throws Exception {
        int ownerId = createUser("owner");
        int renterId = createUser("renter");
        testDatabase.items.insert(ownerId, "Saw", "Hand saw", 1, "available");
        service.delistItem(1, ownerId);

        assertThrows(IllegalStateException.class,
                () -> service.rentItem(1, renterId, "2026-03-01", 1));
        assertEquals("unlisted", text("SELECT status FROM listed_items WHERE item_id = 1"));
        assertEquals(0, count("SELECT COUNT(*) FROM rentals"));
    }

    @Test
    void ownerCannotRentOwnItem() throws Exception {
        int ownerId = createUser("owner");
        testDatabase.items.insert(ownerId, "Projector", "4K projector", 15, "available");

        assertThrows(IllegalArgumentException.class,
                () -> service.rentItem(1, ownerId, "2026-03-01", 2));
        assertEquals("available", text("SELECT status FROM listed_items WHERE item_id = 1"));
        assertEquals(0, count("SELECT COUNT(*) FROM rentals"));
    }

    @Test
    void delistingRequiresExistingItemAndOwner() throws Exception {
        int ownerId = createUser("owner");
        int otherUserId = createUser("other");
        testDatabase.items.insert(ownerId, "Saw", "Hand saw", 1, "available");

        assertThrows(IllegalArgumentException.class, () -> service.delistItem(404, ownerId));
        assertThrows(IllegalArgumentException.class, () -> service.delistItem(1, otherUserId));
        assertEquals("available", text("SELECT status FROM listed_items WHERE item_id = 1"));
    }

    @Test
    void invalidRentalDurationAndDateDoNotChangeDatabase() throws Exception {
        int ownerId = createUser("owner");
        int renterId = createUser("renter");
        testDatabase.items.insert(ownerId, "Saw", "Hand saw", 1, "available");

        assertThrows(IllegalArgumentException.class,
                () -> service.rentItem(1, renterId, "2026-03-01", 0));
        assertThrows(java.time.format.DateTimeParseException.class,
                () -> service.rentItem(1, renterId, "not-a-date", 1));
        assertEquals("available", text("SELECT status FROM listed_items WHERE item_id = 1"));
        assertEquals(0, count("SELECT COUNT(*) FROM rentals"));
    }

    @Test
    void returningRentedItemWithoutActiveRentalFails() throws Exception {
        int ownerId = createUser("owner");
        testDatabase.items.insert(ownerId, "Saw", "Hand saw", 1, "rented");

        assertThrows(IllegalStateException.class, () -> service.returnItem(1, "2026-03-02"));
        assertEquals("rented", text("SELECT status FROM listed_items WHERE item_id = 1"));
    }

    @Test
    void returningAvailableItemFailsWithoutCreatingRental() throws Exception {
        int ownerId = createUser("owner");
        testDatabase.items.insert(ownerId, "Saw", "Hand saw", 1, "available");

        assertThrows(IllegalStateException.class, () -> service.returnItem(1, "2026-03-02"));
        assertEquals("available", text("SELECT status FROM listed_items WHERE item_id = 1"));
        assertEquals(0, count("SELECT COUNT(*) FROM rentals"));
    }

    @Test
    void invalidQueriesAndBlankNamesFailWithoutWriting() throws Exception {
        assertNull(service.getUser("missing"));
        assertThrows(IllegalArgumentException.class, () -> service.registerUser("   "));
        assertThrows(IllegalArgumentException.class, () -> service.getOrRegisterUser(null));
        assertThrows(IllegalArgumentException.class, () -> service.listItem(1, "Saw", "Hand saw", 0));
        assertThrows(IllegalArgumentException.class,
                () -> service.rentItem(404, "renter", "2026-03-01", 1));
        assertThrows(IllegalArgumentException.class, () -> service.returnItem(404, "2026-03-02"));
        assertEquals(0, count("SELECT COUNT(*) FROM users"));
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
