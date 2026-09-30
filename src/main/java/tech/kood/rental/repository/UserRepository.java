package tech.kood.rental.repository;

import tech.kood.rental.domain.User;
import tech.kood.rental.infrastructure.DatabaseConnection;
import tech.kood.rental.repository.exception.ConstraintViolationException;
import tech.kood.rental.repository.exception.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRepository {
    private final DatabaseConnection db;

    public UserRepository(DatabaseConnection db) {
        this.db = db;
    }

    public void insert(String username) {
        String sql = "INSERT INTO users (username) VALUES (?)";
        try (Connection conn = db.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == 19 || e.getMessage().contains("CONSTRAINT")) {
                throw new ConstraintViolationException("Username already exists.", e);
            }
            throw new DatabaseException("Error creating user profile.", e);
        }
    }

    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = db.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getInt("id"), rs.getString("username"), rs.getString("created_at"));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error pulling user identity record.", e);
        }
        return null;
    }
}
