package tech.kood.rental.domain;

public class User {
    public int id;
    public String username;
    public String createdAt;

    public User(int id, String username, String createdAt) {
        this.id = id;
        this.username = username;
        this.createdAt = createdAt;
    }
}
