package tech.kood.rental.transport;

import tech.kood.rental.domain.Item;
import tech.kood.rental.domain.Rental;
import tech.kood.rental.domain.User;
import tech.kood.rental.service.RentalService;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class CliHandler {
    private static final int PAGE_SIZE = 5;

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
        System.out.println("=== Rental tracker ===");
        try {
            currentUser = service.getExistingAccount();
            if (currentUser == null) {
                System.out.print("Username: ");
                String username = readLine();
                if (username == null) return;
                currentUser = service.getOrRegisterUser(username);
            }
        } catch (RuntimeException e) {
            System.out.println("Unable to load account: " + e.getMessage());
            return;
        }

        while (running) {
            showMainMenu();
        }
    }

    private void showMainMenu() {
        System.out.println("\n=== Rental tracker ===");
        System.out.println("1) List an item");
        System.out.println("2) View my inventory");
        System.out.println("3) Record a rental");
        System.out.println("4) Confirm a return");
        System.out.println("5) Exit");
        System.out.print("> ");
        String choice = readLine();
        if (choice == null) return;

        try {
            switch (choice) {
                case "1" -> listItem();
                case "2" -> showInventory();
                case "3" -> showAvailableItems();
                case "4" -> showActiveRentals();
                case "5" -> running = false;
                default -> System.out.println("Please choose an option from 1 to 5.");
            }
        } catch (RuntimeException e) {
            System.out.println("Operation failed: " + e.getMessage());
        }
    }

    private void listItem() {
        System.out.print("Item name: ");
        String name = readRequiredLine();
        if (name == null) return;
        System.out.print("Description: ");
        String description = readRequiredLine();
        if (description == null) return;
        System.out.print("Cost per day: ");
        String cost = readRequiredLine();
        if (cost == null) return;
        service.listItem(currentUser.id, name, description, Double.parseDouble(cost));
        System.out.println("Item listed.");
    }

    private void showInventory() {
        showItemPages("My inventory", () -> service.getUserInventory(currentUser.id),
                item -> item.itemName + "    " + item.status,
                this::showInventoryItemDetail);
    }

    private void showAvailableItems() {
        showItemPages("Record a rental", service::getAvailableItems,
                item -> item.itemName + "    " + String.format("$%.2f/day", item.costPerDay),
                this::showRentalItemDetail);
    }

    private void showItemPages(String title, Supplier<List<Item>> loadItems, Function<Item, String> row,
                               Consumer<Item> onSelect) {
        int page = 0;
        while (running) {
            List<Item> items = loadItems.get();
            int start = page * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, items.size());
            System.out.println("\n=== " + title + " ===");
            if (items.isEmpty()) {
                System.out.println("No items.");
            }
            for (int index = start; index < end; index++) {
                System.out.printf("%d) %s%n", index - start + 1, row.apply(items.get(index)));
            }

            int option = end - start + 1;
            if (page > 0) System.out.printf("%d) Previous page%n", option++);
            if (end < items.size()) System.out.printf("%d) Next page%n", option++);
            System.out.printf("%d) Back to menu%n", option);
            System.out.print("> ");
            Integer selection = readSelection();
            if (selection == null) return;

            if (selection >= 1 && selection <= end - start) {
                onSelect.accept(items.get(start + selection - 1));
            } else if (page > 0 && selection == end - start + 1) {
                page--;
            } else if (end < items.size() && selection == end - start + (page > 0 ? 2 : 1)) {
                page++;
            } else if (selection == option) {
                return;
            } else {
                System.out.println("Invalid selection.");
            }
        }
    }

    private void showInventoryItemDetail(Item item) {
        printItemDetails(item);
        if ("available".equals(item.status)) {
            System.out.println("1) Delist");
        } else if ("unlisted".equals(item.status)) {
            System.out.println("1) Relist");
        }
        System.out.println("2) Back to list");
        System.out.print("> ");
        Integer selection = readSelection();
        if (selection == null || selection == 2) return;
        if (selection == 1 && "available".equals(item.status)) {
            service.delistItem(item.itemId, currentUser.id);
        } else if (selection == 1 && "unlisted".equals(item.status)) {
            service.relistItem(item.itemId, currentUser.id);
        } else {
            System.out.println("Invalid selection.");
        }
    }

    private void showRentalItemDetail(Item item) {
        printItemDetails(item);
        System.out.println("1) Rent");
        System.out.println("2) Back to list");
        System.out.print("> ");
        Integer selection = readSelection();
        if (selection == null || selection != 1) return;

        System.out.print("Renter username: ");
        String renter = readRequiredLine();
        if (renter == null) return;
        System.out.print("Number of days: ");
        String days = readRequiredLine();
        if (days == null) return;
        service.rentItem(item.itemId, renter, LocalDate.now().toString(), Integer.parseInt(days));
        System.out.println("Rental recorded.");
    }

    private void showActiveRentals() {
        int page = 0;
        while (running) {
            List<Rental> rentals = service.getActiveRentals(currentUser.id);
            int start = page * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, rentals.size());
            System.out.println("\n=== Confirm a return ===");
            if (rentals.isEmpty()) System.out.println("No active rentals.");
            for (int index = start; index < end; index++) {
                Rental rental = rentals.get(index);
                Item item = service.getItem(rental.itemId);
                User renter = service.getUserById(rental.renterId);
                System.out.printf("%d) %s    %s%n", index - start + 1, item.itemName, renter.username);
            }

            int option = end - start + 1;
            if (page > 0) System.out.printf("%d) Previous page%n", option++);
            if (end < rentals.size()) System.out.printf("%d) Next page%n", option++);
            System.out.printf("%d) Back to menu%n", option);
            System.out.print("> ");
            Integer selection = readSelection();
            if (selection == null) return;

            if (selection >= 1 && selection <= end - start) {
                showRentalDetail(rentals.get(start + selection - 1));
            } else if (page > 0 && selection == end - start + 1) {
                page--;
            } else if (end < rentals.size() && selection == end - start + (page > 0 ? 2 : 1)) {
                page++;
            } else if (selection == option) {
                return;
            } else {
                System.out.println("Invalid selection.");
            }
        }
    }

    private void showRentalDetail(Rental rental) {
        Item item = service.getItem(rental.itemId);
        User renter = service.getUserById(rental.renterId);
        System.out.println("\n=== " + item.itemName + " ===");
        System.out.println("renter   " + renter.username);
        System.out.println("start    " + rental.startTime);
        System.out.println("end      " + rental.endTime);
        System.out.println("status   " + rental.status);
        System.out.println("1) Confirm return");
        System.out.println("2) Back to list");
        System.out.print("> ");
        Integer selection = readSelection();
        if (selection == null) return;
        if (selection == 1) {
            service.returnItem(item.itemId, LocalDate.now().toString());
            System.out.println("Return confirmed.");
        } else if (selection != 2) {
            System.out.println("Invalid selection.");
        }
    }

    private void printItemDetails(Item item) {
        User owner = service.getUserById(item.ownerId);
        System.out.println("\n=== " + item.itemName + " ===");
        System.out.println("description     " + item.description);
        System.out.println("cost per day    " + item.costPerDay);
        System.out.println("status          " + item.status);
        System.out.println("owner           " + owner.username);
        System.out.println("listed          " + item.createdAt);
    }

    private String readRequiredLine() {
        String line = readLine();
        return line == null || line.isBlank() ? null : line.trim();
    }

    private String readLine() {
        if (!scanner.hasNextLine()) {
            running = false;
            return null;
        }
        return scanner.nextLine();
    }

    private Integer readSelection() {
        String value = readLine();
        if (value == null) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
