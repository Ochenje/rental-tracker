package tech.kood.rental.service;

import tech.kood.rental.domain.Item;
import tech.kood.rental.domain.User;
import tech.kood.rental.domain.Rental;
import tech.kood.rental.repository.ItemRepository;
import tech.kood.rental.repository.RentalRepository;
import tech.kood.rental.repository.UserRepository;

import java.util.List;

public class RentalService {
    private final UserRepository userRepo;
    private final ItemRepository itemRepo;
    private final RentalRepository rentalRepo;

    public RentalService(UserRepository userRepo, ItemRepository itemRepo, RentalRepository rentalRepo) {
        this.userRepo = userRepo;
        this.itemRepo = itemRepo;
        this.rentalRepo = rentalRepo;
    }

    public void registerUser(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be left blank.");
        }
        userRepo.insert(username.trim());
    }

    public User getUser(String username) {
        return userRepo.findByUsername(username);
    }

    public void listItem(int ownerId, String name, String desc, double cost) {
        if (cost <= 0) throw new IllegalArgumentException("Cost allocation metric must evaluate above 0.");
        itemRepo.insert(ownerId, name, desc, cost, "available");
    }

    public List<Item> getAvailableItems() {
        return itemRepo.findAllAvailable();
    }

    // State Machine Guards for Renting
    public void rentItem(int itemId, int renterId, String start, String end) {
        Item item = itemRepo.findById(itemId);
        if (item == null) throw new IllegalArgumentException("Item identity value does not exist.");
        if (!"available".equals(item.status)) {
            throw new IllegalStateException("Item is locked and unavailable for booking rentals.");
        }
        if (item.ownerId == renterId) {
            throw new IllegalArgumentException("Owners cannot rent their own assets.");
        }

        itemRepo.updateStatus(itemId, "rented");
        rentalRepo.insert(itemId, renterId, start, end);
    }

    // State Machine Guards for Returning
    public void returnItem(int itemId, String returnTime) {
        Item item = itemRepo.findById(itemId);
        if (item == null) throw new IllegalArgumentException("Item identity value does not exist.");
        if (!"rented".equals(item.status)) {
            throw new IllegalStateException("Item cannot be returned since it is not currently rented out.");
        }

        Rental activeRental = rentalRepo.findActiveByItemId(itemId);
        if (activeRental == null) {
            throw new IllegalStateException("No active transactional booking linkage maps to this item.");
        }

        rentalRepo.closeRental(activeRental.rentalId, returnTime);
        itemRepo.updateStatus(itemId, "available");
    }
}
