package tech.kood.rental.repository;

import tech.kood.rental.domain.Item;
import tech.kood.rental.infrastructure.DatabaseConnection;
import tech.kood.rental.repository.exception.ConstraintViolationException;
import tech.kood.rental.repository.exception.DatabaseException;
import tech.kood.rental.repository.exception.MappingException;
import tech.kood.rental.repository.exception.NotFoundException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemRepository {
    private final DatabaseConnection db;

    public ItemRepository(DatabaseConnection db) {
        this.db = db;
    }

    public void insert(int ownerId, String name, String desc, double cost, String status) {
        String sql = "INSERT INTO listed_items (owner_id, item_name, description, cost_per_day, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, ownerId);
            pstmt.setString(2, name);
            pstmt.setString(3, desc);
            pstmt.setDouble(4, cost);
            pstmt.setString(5, status);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == 19) {
                throw new ConstraintViolationException("Item properties violate database row schema integrity.", e);
            }
            throw new DatabaseException("Error inserting asset item entry.", e);
        }
    }

    public List<Item> findAllAvailable() {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM listed_items WHERE status = 'available'";
        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                items.add(mapRowToItem(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error matching active public item records.", e);
        }
        return items;
    }

    public Item findById(int itemId) {
        String sql = "SELECT * FROM listed_items WHERE item_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapRowToItem(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving precise item row.", e);
        }
        return null;
    }

    public void updateStatus(int itemId, String newStatus) {
        String sql = "UPDATE listed_items SET status = ? WHERE item_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, itemId);
            if (pstmt.executeUpdate() == 0) {
                throw new NotFoundException("Target catalog item entity does not exist.");
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 19) {
                throw new ConstraintViolationException("State change violates system rules.", e);
            }
            throw new DatabaseException("Error writing item data status.", e);
        }
    }

    private Item mapRowToItem(ResultSet rs) {
        try {
            return new Item(
                    rs.getInt("item_id"),
                    rs.getInt("owner_id"),
                    rs.getString("item_name"),
                    rs.getString("description"),
                    rs.getDouble("cost_per_day"),
                    rs.getString("status"),
                    rs.getString("created_at")
            );
        } catch (SQLException e) {
            throw new MappingException("Failed structural translation from database table into Java Item object.", e);
        }
    }
}
