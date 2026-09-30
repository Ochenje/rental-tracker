package tech.kood.rental.repository;

import tech.kood.rental.domain.Rental;
import tech.kood.rental.infrastructure.DatabaseConnection;
import tech.kood.rental.repository.exception.DatabaseException;
import tech.kood.rental.repository.exception.NotFoundException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RentalRepository {
    private final DatabaseConnection db;

    public RentalRepository(DatabaseConnection db) {
        this.db = db;
    }

    public void insert(int itemId, int renterId, String start, String end) {
        String sql = "INSERT INTO rentals (item_id, renter_id, start_time, end_time, status) VALUES (?, ?, ?, ?, 'active')";
        try (Connection conn = db.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            pstmt.setInt(2, renterId);
            pstmt.setString(3, start);
            pstmt.setString(4, end);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed booking data insert transaction.", e);
        }
    }

    public Rental findActiveByItemId(int itemId) {
        String sql = "SELECT * FROM rentals WHERE item_id = ? AND status = 'active'";
        try (Connection conn = db.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Rental(
                            rs.getInt("rental_id"),
                            rs.getInt("item_id"),
                            rs.getInt("renter_id"),
                            rs.getString("start_time"),
                            rs.getString("end_time"),
                            rs.getString("returned_at"),
                            rs.getString("status")
                    );
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failure searching active rental entries.", e);
        }
        return null;
    }

    public void closeRental(int rentalId, String returnTime) {
        String sql = "UPDATE rentals SET returned_at = ?, status = 'closed' WHERE rental_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, returnTime);
            pstmt.setInt(2, rentalId);
            if (pstmt.executeUpdate() == 0) {
                throw new NotFoundException("Active booking target record not found.");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error processing update status on rental closing transaction.", e);
        }
    }
}
