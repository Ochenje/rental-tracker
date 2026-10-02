package tech.kood.rental.transport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.kood.rental.TestDatabase;
import tech.kood.rental.service.RentalService;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class CliHandlerTest {
    private TestDatabase testDatabase;
    private PrintStream originalOut;
    private ByteArrayOutputStream output;

    @BeforeEach
    void setUp() throws Exception {
        testDatabase = TestDatabase.create();
        originalOut = System.out;
        output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() throws Exception {
        System.setOut(originalOut);
        testDatabase.close();
    }

    @Test
    void firstLaunchListsItemAndRelaunchLoadsSavedAccountWithoutPrompt() throws Exception {
        RentalService service = service();
        new CliHandler(service, new Scanner("owner\n1\nLamp\nDesk lamp\n4.5\n5\n")).run();

        assertEquals(1, count("SELECT COUNT(*) FROM users WHERE username = 'owner'"));
        assertEquals("available", text("SELECT status FROM listed_items WHERE item_name = 'Lamp'"));
        output.reset();

        new CliHandler(service, new Scanner("5\n")).run();

        String relaunchedOutput = capturedOutput();
        assertFalse(relaunchedOutput.contains("Username:"));
        assertTrue(relaunchedOutput.contains("=== Rental tracker ==="));
    }

    @Test
    void fullRentalLifecycleShowsDetailsAndSupportsDelisting() throws Exception {
        int ownerId = createUser("owner");
        testDatabase.items.insert(ownerId, "Tripod", "Camera tripod", 4.5, "available");
        RentalService service = service();
        String input = String.join("\n",
                "2", "1", "2", "2",
                "3", "1", "1", "renter", "2", "1",
                "4", "1", "1", "1",
                "2", "1", "3", "1", "1", "1", "1", "1", "1", "2",
                "5", "");

        new CliHandler(service, new Scanner(input)).run();

        assertEquals("unlisted", text("SELECT status FROM listed_items WHERE item_id = 1"));
        assertEquals("closed", text("SELECT status FROM rentals WHERE item_id = 1"));
        assertNotNull(text("SELECT returned_at FROM rentals WHERE item_id = 1"));
        String capturedOutput = capturedOutput();
        assertTrue(capturedOutput.contains("owner           owner"));
        assertTrue(capturedOutput.contains("description     Camera tripod"));
        assertTrue(capturedOutput.contains("renter   renter"));
        assertTrue(capturedOutput.contains("end      "));
        assertTrue(capturedOutput.contains("status   active"));
        assertTrue(capturedOutput.contains("Return confirmed."));
    }

    @Test
    void inventoryAndAvailableListsPaginateForwardAndBackward() throws Exception {
        int ownerId = createUser("owner");
        for (int i = 1; i <= 11; i++) {
            testDatabase.items.insert(ownerId, "Tool " + i, "Description " + i, i, "available");
        }
        RentalService service = service();
        String input = String.join("\n",
                "2", "6", "99", "7", "2", "6", "7",
                "3", "6", "99", "7", "2", "6", "7",
                "5", "");

        new CliHandler(service, new Scanner(input)).run();

        String capturedOutput = capturedOutput();
        assertTrue(capturedOutput.contains("6) Next page"));
        assertTrue(capturedOutput.contains("2) Previous page"));
        assertTrue(capturedOutput.contains("Tool 6    available"));
        assertTrue(capturedOutput.contains("Tool 6    $6.00/day"));
    }

    @Test
    void returnListIsOrderedByDueDateAndShowsRentalDetails() throws Exception {
        int ownerId = createUser("owner");
        int renterId = createUser("renter");
        testDatabase.items.insert(ownerId, "Later due", "Long rental", 1, "available");
        testDatabase.items.insert(ownerId, "Earlier due", "Short rental", 1, "available");
        RentalService service = service();
        service.rentItem(1, renterId, "2026-03-01", 8);
        service.rentItem(2, renterId, "2026-03-01", 2);

        new CliHandler(service, new Scanner("4\n1\n2\n3\n5\n")).run();

        String capturedOutput = capturedOutput();
        assertTrue(capturedOutput.indexOf("1) Earlier due    renter")
                < capturedOutput.indexOf("2) Later due    renter"));
        assertTrue(capturedOutput.contains("start    2026-03-01"));
        assertTrue(capturedOutput.contains("end      2026-03-03"));
    }

    @Test
    void invalidMenuAndListInputReturnsCleanlyAndExitStops() throws Exception {
        createUser("owner");
        new CliHandler(service(), new Scanner("bad\n2\nbad\n5\n")).run();

        assertTrue(capturedOutput().contains("Please choose an option from 1 to 5."));
        assertTrue(capturedOutput().contains("Invalid selection."));
        assertEquals(3, occurrences(capturedOutput(), "=== Rental tracker ==="));
    }

    @Test
    void blankListingFieldsAndInvalidPriceDoNotCreateItems() throws Exception {
        createUser("owner");
        String input = String.join("\n",
                "1", "",
                "1", "Lamp", "",
                "1", "Lamp", "Desk lamp", "",
                "1", "Lamp", "Desk lamp", "not-a-price",
                "5", "");

        new CliHandler(service(), new Scanner(input)).run();

        assertEquals(0, count("SELECT COUNT(*) FROM listed_items"));
        assertTrue(capturedOutput().contains("Operation failed: For input string: \"not-a-price\""));
    }

    @Test
    void rentalDetailsCanBeCancelledAndMissingRenterInputsAreHandled() throws Exception {
        int ownerId = createUser("owner");
        testDatabase.items.insert(ownerId, "Lamp", "Desk lamp", 4, "available");
        RentalService service = service();
        new CliHandler(service, new Scanner("3\n1\n2\n2\n5\n")).run();
        assertEquals(0, count("SELECT COUNT(*) FROM rentals"));

        new CliHandler(service, new Scanner("3\n1\n1\n\n1\n5\n")).run();
        new CliHandler(service, new Scanner("3\n1\n1\nrenter\n\n1\n5\n")).run();

        assertEquals(0, count("SELECT COUNT(*) FROM rentals"));
    }

    @Test
    void rentedInventoryAndReturnListPaginationAreSupported() throws Exception {
        int ownerId = createUser("owner");
        int renterId = createUser("renter");
        for (int i = 1; i <= 11; i++) {
            testDatabase.items.insert(ownerId, "Tool " + i, "Description " + i, i, "available");
            service().rentItem(i, renterId, "2026-03-01", i);
        }
        new CliHandler(service(), new Scanner("2\n1\n1\n2\n7\n5\n")).run();
        new CliHandler(service(), new Scanner("4\ninvalid\n1\n3\n2\n7\n5\n")).run();
        output.reset();

        new CliHandler(service(), new Scanner("4\n6\n7\n2\n6\n7\n5\n")).run();

        assertTrue(capturedOutput().contains("6) Next page"));
        assertTrue(capturedOutput().contains("2) Previous page"));
        assertTrue(capturedOutput().contains("Tool 6    renter"));
    }

    @Test
    void endOfInputExitsFirstRunAndNestedScreensSafely() throws Exception {
        new CliHandler(service(), new Scanner("")).run();
        assertTrue(capturedOutput().contains("Username:"));

        int ownerId = createUser("owner");
        testDatabase.items.insert(ownerId, "Lamp", "Desk lamp", 4, "available");
        output.reset();
        new CliHandler(service(), new Scanner("2\n1\n")).run();
        assertTrue(capturedOutput().contains("2) Back to list"));

        output.reset();
        new CliHandler(service(), new Scanner("3\n1\n")).run();
        assertTrue(capturedOutput().contains("1) Rent"));

        output.reset();
        new CliHandler(service(), new Scanner("1\n")).run();
        assertTrue(capturedOutput().contains("Item name:"));
    }

    @Test
    void startupDatabaseFailureIsReportedInsteadOfCrashing() throws Exception {
        try (Connection connection = testDatabase.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE users");
        }

        new CliHandler(service(), new Scanner("")).run();

        assertTrue(capturedOutput().contains("Unable to load account:"));
    }

    private RentalService service() {
        return new RentalService(testDatabase.users, testDatabase.items, testDatabase.rentals);
    }

    private int createUser(String username) {
        testDatabase.users.insert(username);
        return testDatabase.users.findByUsername(username).id;
    }

    private int count(String sql) throws Exception {
        try (Connection connection = testDatabase.getConnection();
             Statement statement = connection.createStatement();
             var result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }

    private String text(String sql) throws Exception {
        try (Connection connection = testDatabase.getConnection();
             Statement statement = connection.createStatement();
             var result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getString(1);
        }
    }

    private String capturedOutput() {
        return output.toString(StandardCharsets.UTF_8);
    }

    private int occurrences(String text, String target) {
        return (text.length() - text.replace(target, "").length()) / target.length();
    }
}
