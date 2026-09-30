package tech.kood.rental.transport;

import tech.kood.rental.domain.Item;
import tech.kood.rental.domain.User;
import tech.kood.rental.service.RentalService;

import java.util.List;
import java.util.Scanner;

public class CliHandler {
    private final RentalService service;
    private final Scanner scanner;
    private User currentUser;
    private boolean running = true;

    public CliHandler(RentalService service) {
        this(service, new Scanner(System.in));
    }

    public CliHandler(RentalService service, Scanner scanner) {
        this.service = service;
        this.scanner = scanner;
    }

    public void run() {
        System.out.println("Welcome to Nairobi Rental Tracker Terminal Workspace.");
        while (running) {
            if (currentUser == null) {
                showAuthMenu();
            } else {
                showMainMenu();
            }
        }
    }

    private void showAuthMenu() {
        System.out.println("\n1. Create Profile\n2. Authenticate Sign-In\n3. Exit System");
        System.out.print("Action Selection -> ");
        String selection = scanner.nextLine();

        try {
            if ("1".equals(selection)) {
                System.out.print("Choose unique username: ");
                String name = scanner.nextLine();
                currentUser = service.getOrRegisterUser(name);
                System.out.println("Account ready for: " + currentUser.username);
            } else if ("2".equals(selection)) {
                System.out.print("Input username: ");
                String name = scanner.nextLine();
                User user = service.getUser(name);
                if (user != null) {
                    currentUser = user;
                    System.out.println("Authentication authenticated! Session active for: " + currentUser.username);
                } else {
                    System.out.println("No matching user profile record.");
                }
            } else if ("3".equals(selection)) {
                running = false;
            } else {
                System.out.println("Invalid selection parameter index.");
            }
        } catch (Exception e) {
            System.out.println("Operation Exception Error: " + e.getMessage());
        }
    }

    private void showMainMenu() {
        System.out.println("\n--- Connected Workspace Dashboard ---");
        System.out.println("1. List an Item Asset\n2. View Public Listings\n3. View Inventory / Delist\n4. Rent an Item\n5. Return an Item\n6. Sign Out");
        System.out.print("Action Selection -> ");
        String choice = scanner.nextLine();

        try {
            switch (choice) {
                case "1" -> {
                    System.out.print("Item name: "); String name = scanner.nextLine();
                    System.out.print("Description context: "); String desc = scanner.nextLine();
                    System.out.print("Daily pricing cost rate: "); double cost = Double.parseDouble(scanner.nextLine());
                    service.listItem(currentUser.id, name, desc, cost);
                    System.out.println("Catalog asset successfully deployed into circulation marketplace.");
                }
                case "2" -> {
                    List<Item> catalog = service.getAvailableItems();
                    if (catalog.isEmpty()) {
                        System.out.println("No available items listed at the moment.");
                    } else {
                        catalog.forEach(i -> System.out.printf("[%d] %s - %s ($%.2f/day)%n", i.itemId, i.itemName, i.description, i.costPerDay));
                    }
                }
                case "3" -> {
                    List<Item> inventory = service.getUserInventory(currentUser.id);
                    if (inventory.isEmpty()) {
                        System.out.println("Your inventory is empty.");
                        break;
                    }
                    inventory.forEach(i -> System.out.printf("[%d] %s (%s)%n", i.itemId, i.itemName, i.status));
                    System.out.print("Item ID to delist (0 to return): "); int id = Integer.parseInt(scanner.nextLine());
                    if (id != 0) service.delistItem(id, currentUser.id);
                }
                case "4" -> {
                    System.out.print("Enter Item ID target: "); int id = Integer.parseInt(scanner.nextLine());
                    System.out.print("Renter username: "); String renter = scanner.nextLine();
                    System.out.print("Start date (YYYY-MM-DD): "); String start = scanner.nextLine();
                    System.out.print("Number of rental days: "); int days = Integer.parseInt(scanner.nextLine());
                    service.rentItem(id, renter, start, days);
                    System.out.println("Lease tracking parameters registered safely.");
                }
                case "5" -> {
                    System.out.print("Enter Item ID to return: "); int id = Integer.parseInt(scanner.nextLine());
                    System.out.print("Return time execution timestamp: "); String time = scanner.nextLine();
                    service.returnItem(id, time);
                    System.out.println("Item log successfully closed and marked safe for circulation again.");
                }
                case "6" -> {
                    currentUser = null;
                    System.out.println("Logged out securely.");
                }
                default -> System.out.println("Invalid selection parameter index.");
            }
        } catch (Exception e) {
            System.out.println("Operation Failure: " + e.getMessage());
        }
    }
}
