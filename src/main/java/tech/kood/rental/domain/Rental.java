package tech.kood.rental.domain;

public class Rental {
    public int rentalId;
    public int itemId;
    public int renterId;
    public String startTime;
    public String endTime;
    public String returnedAt;
    public String status;

    public Rental(int rentalId, int itemId, int renterId, String startTime, String endTime, String returnedAt, String status) {
        this.rentalId = rentalId;
        this.itemId = itemId;
        this.renterId = renterId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.returnedAt = returnedAt;
        this.status = status;
    }
}
