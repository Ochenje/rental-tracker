package tech.kood.rental.service;

import tech.kood.rental.domain.Item;
import tech.kood.rental.domain.Rental;
import tech.kood.rental.domain.User;
import tech.kood.rental.repository.ItemRepository;
import tech.kood.rental.repository.RentalRepository;
import tech.kood.rental.repository.UserRepository;

import java.time.LocalDate;
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

    public User getOrRegisterUser(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be left blank.");
        }
        String normalizedUsername = username.trim();
        User existingUser = userRepo.findByUsername(normalizedUsername);
        if (existingUser != null) return existingUser;
        userRepo.insert(normalizedUsername);
        return userRepo.findByUsername(normalizedUsername);
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

    public List<Item> getUserInventory(int ownerId) {
        return itemRepo.findByOwnerId(ownerId);
    }

    public void delistItem(int itemId, int ownerId) {
        Item item = itemRepo.findById(itemId);
        if (item == null) throw new IllegalArgumentException("Item identity value does not exist.");
        if (item.ownerId != ownerId) throw new IllegalArgumentException("Only the owner can delist this item.");
        itemRepo.updateStatus(itemId, "unlisted");
    }

    public void rentItem(int itemId, int renterId, String start, int rentalDays) {
        Item item = requireAvailableItem(itemId);
        String end = calculateEndDate(start, rentalDays);
        recordRental(item, renterId, start, end);
    }

    public void rentItem(int itemId, String renterUsername, String start, int rentalDays) {
        Item item = requireAvailableItem(itemId);
        String end = calculateEndDate(start, rentalDays);
        User renter = getOrRegisterUser(renterUsername);
        recordRental(item, renter.id, start, end);
    }

    public void returnItem(int itemId, String returnTime) {
        Item item = itemRepo.findById(itemId);
        if (item == null) throw new IllegalArgumentException("Item identity value does not exist.");
        if (!"rented".equals(item.status) && !"unlisted".equals(item.status)) {
            throw new IllegalStateException("Item cannot be returned since it is not currently rented out.");
        }

        Rental activeRental = rentalRepo.findActiveByItemId(itemId);
        if (activeRental == null) {
            throw new IllegalStateException("No active transactional booking linkage maps to this item.");
        }

        rentalRepo.closeRental(activeRental.rentalId, returnTime);
        if ("rented".equals(item.status)) {
            itemRepo.updateStatus(itemId, "available");
        }
    }

    private Item requireAvailableItem(int itemId) {
        Item item = itemRepo.findById(itemId);
        if (item == null) throw new IllegalArgumentException("Item identity value does not exist.");
        if (!"available".equals(item.status)) {
            throw new IllegalStateException("Item is locked and unavailable for booking rentals.");
        }
        return item;
    }

    private void recordRental(Item item, int renterId, String start, String end) {
        if (item.ownerId == renterId) {
            throw new IllegalArgumentException("Owners cannot rent their own assets.");
        }
        itemRepo.updateStatus(item.itemId, "rented");
        rentalRepo.insert(item.itemId, renterId, start, end);
    }

    private String calculateEndDate(String start, int rentalDays) {
        if (rentalDays <= 0) throw new IllegalArgumentException("Rental duration must be greater than zero days.");
        return LocalDate.parse(start).plusDays(rentalDays).toString();
    }
}
