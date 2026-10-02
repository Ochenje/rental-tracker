package tech.kood.rental;

import tech.kood.rental.infrastructure.DatabaseConnection;
import tech.kood.rental.repository.ItemRepository;
import tech.kood.rental.repository.RentalRepository;
import tech.kood.rental.repository.UserRepository;
import tech.kood.rental.service.RentalService;
import tech.kood.rental.transport.CliHandler;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        String databasePath = args.length == 0 ? System.getProperty("rental.database", "app.db") : args[0];
        DatabaseConnection dbConnection = new DatabaseConnection(databasePath);
        if (!initializeDatabase(dbConnection, Main.class.getClassLoader().getResourceAsStream("schema.sql"))) {
            return;
        }

        UserRepository userRepo = new UserRepository(dbConnection);
        ItemRepository itemRepo = new ItemRepository(dbConnection);
        RentalRepository rentalRepo = new RentalRepository(dbConnection);
        RentalService rentalService = new RentalService(userRepo, itemRepo, rentalRepo);

        CliHandler cliHandler = new CliHandler(rentalService);
        cliHandler.run();
    }

    static boolean initializeDatabase(DatabaseConnection db, InputStream schema) {
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             InputStream is = schema) {
            if (is == null) return true;
            try (Scanner scanner = new Scanner(is).useDelimiter(";")) {
                while (scanner.hasNext()) {
                    String sql = scanner.next().trim();
                    if (!sql.isEmpty()) {
                        stmt.execute(sql);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            return false;
        }
    }
}
