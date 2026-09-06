package game.dao;

import game.db.DatabaseConnection;
import game.model.LeaderboardEntry;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ScoreDAO {

    /** Inserts a first score, or raises HighScore only if the new run beats it. */
    public void submitScore(int playerId, int newScore) throws SQLException {
        String select = "SELECT ScoreID, HighScore FROM Scores WHERE PlayerID = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {

            try (PreparedStatement ps = conn.prepareStatement(select)) {
                ps.setInt(1, playerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int existing = rs.getInt("HighScore");
                        if (newScore > existing) {
                            try (PreparedStatement update = conn.prepareStatement(
                                    "UPDATE Scores SET HighScore = ? WHERE PlayerID = ?")) {
                                update.setInt(1, newScore);
                                update.setInt(2, playerId);
                                update.executeUpdate();
                            }
                        }
                    } else {
                        try (PreparedStatement insert = conn.prepareStatement(
                                "INSERT INTO Scores (PlayerID, HighScore, `Rank`) VALUES (?, ?, 0)")) {
                            insert.setInt(1, playerId);
                            insert.setInt(2, newScore);
                            insert.executeUpdate();
                        }
                    }
                }
            }
            recalculateRanks(conn);
        }
    }

    /** Re-ranks every player 1..N by HighScore descending. */
    private void recalculateRanks(Connection conn) throws SQLException {
        String selectOrdered = "SELECT ScoreID FROM Scores ORDER BY HighScore DESC";
        List<Integer> orderedIds = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(selectOrdered)) {
            while (rs.next()) {
                orderedIds.add(rs.getInt("ScoreID"));
            }
        }
        try (PreparedStatement update = conn.prepareStatement("UPDATE Scores SET `Rank` = ? WHERE ScoreID = ?")) {
            int rank = 1;
            for (int scoreId : orderedIds) {
                update.setInt(1, rank++);
                update.setInt(2, scoreId);
                update.addBatch();
            }
            update.executeBatch();
        }
    }

    public List<String> getLeaderboard() throws SQLException {
        List<String> board = new ArrayList<>();
        String sql = "SELECT s.`Rank`, p.Name, s.HighScore FROM Scores s " +
                     "JOIN Players p ON s.PlayerID = p.PlayerID " +
                     "ORDER BY s.`Rank` ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                board.add(String.format("#%d  %-20s %d pts",
                        rs.getInt("Rank"), rs.getString("Name"), rs.getInt("HighScore")));
            }
        }
        return board;
    }

    public List<LeaderboardEntry> getLeaderboardEntries() throws SQLException {
        List<LeaderboardEntry> board = new ArrayList<>();
        String sql = "SELECT s.`Rank`, p.Name, s.HighScore FROM Scores s JOIN Players p ON s.PlayerID = p.PlayerID ORDER BY s.`Rank` ASC";
        try (Connection conn = DatabaseConnection.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) board.add(new LeaderboardEntry(rs.getInt("Rank"), rs.getString("Name"), rs.getInt("HighScore")));
        }
        return board;
    }
}
