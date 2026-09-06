package game.dao;

import game.db.DatabaseConnection;
import game.model.Inventory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InventoryDAO {

    public Inventory addItem(Inventory item) throws SQLException {
        String sql = "INSERT INTO Inventory (PlayerID, WeaponName, Damage, Quantity) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, item.getPlayerId());
            ps.setString(2, item.getWeaponName());
            ps.setInt(3, item.getDamage());
            ps.setInt(4, item.getQuantity());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    item.setItemId(keys.getInt(1));
                }
            }
        }
        return item;
    }

    public List<Inventory> getInventoryByPlayer(int playerId) throws SQLException {
        List<Inventory> items = new ArrayList<>();
        String sql = "SELECT * FROM Inventory WHERE PlayerID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new Inventory(
                            rs.getInt("ItemID"),
                            rs.getInt("PlayerID"),
                            rs.getString("WeaponName"),
                            rs.getInt("Damage"),
                            rs.getInt("Quantity")
                    ));
                }
            }
        }
        return items;
    }

    public boolean updateQuantity(int itemId, int quantity) throws SQLException {
        String sql = "UPDATE Inventory SET Quantity = ? WHERE ItemID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, itemId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteItem(int itemId) throws SQLException {
        String sql = "DELETE FROM Inventory WHERE ItemID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            return ps.executeUpdate() > 0;
        }
    }
}
