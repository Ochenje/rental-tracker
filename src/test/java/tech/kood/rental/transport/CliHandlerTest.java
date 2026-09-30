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
    void cliDemonstratesListingDelistingRentalReturnAndPersistentAccounts() throws Exception {
        RentalService service = new RentalService(testDatabase.users, testDatabase.items, testDatabase.rentals);
        String input = String.join("\n",
                "1", "owner",
                "1", "Tripod", "Camera tripod", "4.5",
                "2",
                "3", "1",
                "2",
                "1", "Camera", "Digital camera", "10",
                "4", "2", "renter", "2026-03-01", "3",
                "5", "2", "2026-03-04",
                "6",
                "1", "owner",
                "6",
                "2", "owner",
                "6", "3", "");

        new CliHandler(service, new Scanner(input)).run();

        assertEquals(2, count("SELECT COUNT(*) FROM users"));
        assertEquals("unlisted", text("SELECT status FROM listed_items WHERE item_name = 'Tripod'"));
        assertEquals("available", text("SELECT status FROM listed_items WHERE item_name = 'Camera'"));
        assertEquals("closed", text("SELECT status FROM rentals WHERE item_id = 2"));
        assertEquals("2026-03-04", text("SELECT end_time FROM rentals WHERE item_id = 2"));
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("No available items listed"));
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Account ready for: owner"));
    }

    @Test
    void cliHandlesEmptyInventoryAndInvalidNumericInput() throws Exception {
        RentalService service = new RentalService(testDatabase.users, testDatabase.items, testDatabase.rentals);
        String input = String.join("\n", "1", "owner", "3", "6", "3", "");

        new CliHandler(service, new Scanner(input)).run();

        assertEquals(0, count("SELECT COUNT(*) FROM listed_items"));
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Your inventory is empty."));
    }

    @Test
    void cliReportsValidationAndAuthenticationErrors() throws Exception {
        RentalService service = new RentalService(testDatabase.users, testDatabase.items, testDatabase.rentals);
        String input = String.join("\n", "2", "missing", "1", "   ", "1", "owner", "1", "Camera", "Item", "bad", "6", "3", "");

        new CliHandler(service, new Scanner(input)).run();

        String capturedOutput = output.toString(StandardCharsets.UTF_8);
        assertTrue(capturedOutput.contains("No matching user profile record."));
        assertTrue(capturedOutput.contains("Operation Exception Error: Username cannot be left blank."));
        assertTrue(capturedOutput.contains("Operation Failure: For input string: \"bad\""));
        assertEquals(1, count("SELECT COUNT(*) FROM users"));
        assertEquals(0, count("SELECT COUNT(*) FROM listed_items"));
    }

    @Test
    void cliCanBeRunAgainAndReusesTheSameAccount() throws Exception {
        RentalService service = new RentalService(testDatabase.users, testDatabase.items, testDatabase.rentals);
        new CliHandler(service, new Scanner("1\nowner\n6\n3\n")).run();
        new CliHandler(service, new Scanner("1\nowner\n6\n3\n")).run();

        assertEquals(1, count("SELECT COUNT(*) FROM users WHERE username = 'owner'"));
        assertEquals(2, occurrences(output.toString(StandardCharsets.UTF_8), "Account ready for: owner"));
    }

    @Test
    void cliSupportsInventoryCancelAndRejectsUnknownChoices() throws Exception {
        testDatabase.users.insert("owner");
        int ownerId = testDatabase.users.findByUsername("owner").id;
        testDatabase.items.insert(ownerId, "Saw", "Hand saw", 1, "available");
        RentalService service = new RentalService(testDatabase.users, testDatabase.items, testDatabase.rentals);
        String input = String.join("\n", "invalid", "2", "owner", "3", "0", "invalid", "6", "3", "");

        new CliHandler(service, new Scanner(input)).run();

        assertEquals("available", text("SELECT status FROM listed_items WHERE item_id = 1"));
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Invalid selection parameter index."));
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

    private int occurrences(String text, String target) {
        return (text.length() - text.replace(target, "").length()) / target.length();
    }
}