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
        DatabaseConnection dbConnection = new DatabaseConnection("app.db");
        initializeDatabase(dbConnection);

        UserRepository userRepo = new UserRepository(dbConnection);
        ItemRepository itemRepo = new ItemRepository(dbConnection);
        RentalRepository rentalRepo = new RentalRepository(dbConnection);
        RentalService rentalService = new RentalService(userRepo, itemRepo, rentalRepo);

        CliHandler cliHandler = new CliHandler(rentalService);
        cliHandler.run();
    }

    private static void initializeDatabase(DatabaseConnection db) {
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             InputStream is = Main.class.getClassLoader().getResourceAsStream("schema.sql")) {
            
            if (is == null) return;
            Scanner scanner = new Scanner(is).useDelimiter(";");
            while (scanner.hasNext()) {
                String sql = scanner.next().trim();
                if (!sql.isEmpty()) {
                    stmt.execute(sql);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
        }
    }
}
