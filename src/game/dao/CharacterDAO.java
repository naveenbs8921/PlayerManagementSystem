package game.dao;

import game.db.DatabaseConnection;
import game.model.Archer;
import game.model.Character;
import game.model.Mage;
import game.model.Warrior;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CharacterDAO {

    public Character addCharacter(Character character) throws SQLException {
        String sql = "INSERT INTO Characters (PlayerID, CharacterName, Class, Health, Attack, Defense, Level) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, character.getPlayerId());
            ps.setString(2, character.getCharacterName());
            ps.setString(3, character.getCharacterClass());
            ps.setInt(4, character.getHealth());
            ps.setInt(5, character.getAttackStat());
            ps.setInt(6, character.getDefense());
            ps.setInt(7, character.getLevel());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    character.setCharacterId(keys.getInt(1));
                }
            }
        }
        return character;
    }

    public List<Character> getCharactersByPlayer(int playerId) throws SQLException {
        List<Character> characters = new ArrayList<>();
        String sql = "SELECT * FROM Characters WHERE PlayerID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    characters.add(mapRow(rs));
                }
            }
        }
        return characters;
    }

    public boolean updateCharacter(Character character) throws SQLException {
        String sql = "UPDATE Characters SET CharacterName=?, Health=?, Attack=?, Defense=?, Level=? " +
                     "WHERE CharacterID=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, character.getCharacterName());
            ps.setInt(2, character.getHealth());
            ps.setInt(3, character.getAttackStat());
            ps.setInt(4, character.getDefense());
            ps.setInt(5, character.getLevel());
            ps.setInt(6, character.getCharacterId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteCharacter(int characterId) throws SQLException {
        String sql = "DELETE FROM Characters WHERE CharacterID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, characterId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Factory step: turns a DB row back into the correct polymorphic subclass. */
    private Character mapRow(ResultSet rs) throws SQLException {
        int characterId = rs.getInt("CharacterID");
        int playerId = rs.getInt("PlayerID");
        String name = rs.getString("CharacterName");
        String cls = rs.getString("Class");
        int health = rs.getInt("Health");
        int attack = rs.getInt("Attack");
        int defense = rs.getInt("Defense");
        int level = rs.getInt("Level");

        return switch (cls) {
            case "Mage" -> new Mage(characterId, playerId, name, health, attack, defense, level);
            case "Archer" -> new Archer(characterId, playerId, name, health, attack, defense, level);
            default -> new Warrior(characterId, playerId, name, health, attack, defense, level);
        };
    }
}
