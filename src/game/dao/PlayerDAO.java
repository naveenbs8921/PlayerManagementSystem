package game.dao;

import game.db.DatabaseConnection;
import game.model.Player;

import java.sql.*;

public class PlayerDAO {

    public Player registerPlayer(Player player) throws SQLException {
        String sql = "INSERT INTO Players (Name, Email, Password, Level, XP, Coins) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, player.getName());
            ps.setString(2, player.getEmail());
            ps.setString(3, player.getPassword());
            ps.setInt(4, player.getLevel());
            ps.setInt(5, player.getXp());
            ps.setInt(6, player.getCoins());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    player.setPlayerId(keys.getInt(1));
                }
            }
        }
        return player;
    }

    public Player login(String email, String password) throws SQLException {
        String sql = "SELECT * FROM Players WHERE Email = ? AND Password = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null; // invalid credentials
    }

    public Player getPlayerById(int playerId) throws SQLException {
        String sql = "SELECT * FROM Players WHERE PlayerID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public boolean updatePlayer(Player player) throws SQLException {
        String sql = "UPDATE Players SET Name = ?, Email = ?, Password = ?, Level = ?, XP = ?, Coins = ? " +
                     "WHERE PlayerID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, player.getName());
            ps.setString(2, player.getEmail());
            ps.setString(3, player.getPassword());
            ps.setInt(4, player.getLevel());
            ps.setInt(5, player.getXp());
            ps.setInt(6, player.getCoins());
            ps.setInt(7, player.getPlayerId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deletePlayer(int playerId) throws SQLException {
        String sql = "DELETE FROM Players WHERE PlayerID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playerId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT 1 FROM Players WHERE Email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Player mapRow(ResultSet rs) throws SQLException {
        return new Player(
                rs.getInt("PlayerID"),
                rs.getString("Name"),
                rs.getString("Email"),
                rs.getString("Password"),
                rs.getInt("Level"),
                rs.getInt("XP"),
                rs.getInt("Coins")
        );
    }
}
