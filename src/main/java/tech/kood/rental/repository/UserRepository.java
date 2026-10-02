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
            if (ConstraintViolationException.isConstraintViolation(e)) {
                throw ConstraintViolationException.from(e);
            }
            throw new DatabaseException("Error creating user profile.", e);
        }
    }

    public User findById(int userId) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getInt("id"), rs.getString("username"), rs.getString("created_at"));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving user identity record.", e);
        }
        return null;
    }

    public User findFirst() {
        String sql = "SELECT * FROM users ORDER BY id LIMIT 1";
        try (Connection conn = db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return new User(rs.getInt("id"), rs.getString("username"), rs.getString("created_at"));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving the existing account.", e);
        }
        return null;
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
