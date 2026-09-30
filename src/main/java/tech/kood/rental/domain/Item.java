package tech.kood.rental.domain;

public class Item {
    public int itemId;
    public int ownerId;
    public String itemName;
    public String description;
    public double costPerDay;
    public String status; 
    public String createdAt;

    public Item(int itemId, int ownerId, String itemName, String description, double costPerDay, String status, String createdAt) {
        this.itemId = itemId;
        this.ownerId = ownerId;
        this.itemName = itemName;
        this.description = description;
        this.costPerDay = costPerDay;
        this.status = status;
        this.createdAt = createdAt;
    }
}
