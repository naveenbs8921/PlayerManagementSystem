package game.dao;

import game.db.DatabaseConnection;
import game.model.Achievement;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AchievementDAO {

    public Achievement addAchievement(Achievement achievement) throws SQLException {
        String sql = "INSERT INTO Achievements (PlayerID, Title, Description, Unlocked) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, achievement.getPlayerId());
            ps.setString(2, achievement.getTitle());
            ps.setString(3, achievement.getDescription());
            ps.setBoolean(4, achievement.isUnlocked());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    achievement.setAchievementId(keys.getInt(1));
                }
            }
        }
        return achievement;
    }

    public boolean unlockAchievement(int achievementId) throws SQLException {
        String sql = "UPDATE Achievements SET Unlocked = TRUE WHERE AchievementID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, achievementId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Achievement> getAchievementsByPlayer(int playerId) throws SQLException {
        List<Achievement> list = new ArrayList<>();
        String sql = "SELECT * FROM Achievements WHERE PlayerID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Achievement(
                            rs.getInt("AchievementID"),
                            rs.getInt("PlayerID"),
                            rs.getString("Title"),
                            rs.getString("Description"),
                            rs.getBoolean("Unlocked")
                    ));
                }
            }
        }
        return list;
    }
}
