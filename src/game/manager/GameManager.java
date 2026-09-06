package game.manager;

import game.dao.*;
import game.model.Achievement;
import game.model.Archer;
import game.model.Bow;
import game.model.Inventory;
import game.model.Mage;
import game.model.Player;
import game.model.Sword;
import game.model.Warrior;
import game.model.Weapon;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

/**
 * Drives the full workflow:
 * Login -> Dashboard -> Choose Character -> Manage Inventory
 * -> Update Score -> Save to DB -> Leaderboard
 */
public class GameManager {

    private final Scanner scanner = new Scanner(System.in);
    private final PlayerDAO playerDAO = new PlayerDAO();
    private final CharacterDAO characterDAO = new CharacterDAO();
    private final InventoryDAO inventoryDAO = new InventoryDAO();
    private final ScoreDAO scoreDAO = new ScoreDAO();
    private final AchievementDAO achievementDAO = new AchievementDAO();

    private Player currentPlayer;

    public void start() {
        System.out.println("=== PLAYER ACCOUNT & GAME MANAGEMENT SYSTEM ===");
        boolean running = true;
        while (running) {
            System.out.println("\n1) Register  2) Login  3) Exit");
            switch (prompt("Choose: ")) {
                case "1" -> register();
                case "2" -> { if (login()) dashboard(); }
                case "3" -> running = false;
                default -> System.out.println("Invalid option.");
            }
        }
        System.out.println("Goodbye!");
    }

    // ---------------- Player Management ----------------

    private void register() {
        try {
            String name = prompt("Name: ");
            String email = prompt("Email: ");
            if (playerDAO.emailExists(email)) {
                System.out.println("That email is already registered.");
                return;
            }
            String password = prompt("Password: ");
            Player player = new Player(name, email, password);
            playerDAO.registerPlayer(player);
            System.out.println("Registered! " + player);
        } catch (SQLException e) {
            handleDbError(e);
        }
    }

    private boolean login() {
        try {
            String email = prompt("Email: ");
            String password = prompt("Password: ");
            Player player = playerDAO.login(email, password);
            if (player == null) {
                System.out.println("Invalid credentials.");
                return false;
            }
            currentPlayer = player;
            System.out.println("Welcome back, " + player.getName() + "!");
            return true;
        } catch (SQLException e) {
            handleDbError(e);
            return false;
        }
    }

    private void editProfile() {
        try {
            String name = prompt("New name (blank = keep '" + currentPlayer.getName() + "'): ");
            if (!name.isBlank()) currentPlayer.setName(name);
            String email = prompt("New email (blank = keep current): ");
            if (!email.isBlank()) currentPlayer.setEmail(email);
            String password = prompt("New password (blank = keep current): ");
            if (!password.isBlank()) currentPlayer.setPassword(password);
            playerDAO.updatePlayer(currentPlayer);
            System.out.println("Profile updated: " + currentPlayer);
        } catch (SQLException e) {
            handleDbError(e);
        }
    }

    private boolean deleteAccount() {
        try {
            if (prompt("Type YES to permanently delete your account: ").equalsIgnoreCase("YES")) {
                playerDAO.deletePlayer(currentPlayer.getPlayerId());
                System.out.println("Account deleted.");
                currentPlayer = null;
                return true;
            }
        } catch (SQLException e) {
            handleDbError(e);
        }
        return false;
    }

    // ---------------- Dashboard / workflow ----------------

    private void dashboard() {
        boolean inSession = true;
        while (inSession && currentPlayer != null) {
            System.out.println("\n--- DASHBOARD: " + currentPlayer + " ---");
            System.out.println("1) Choose/Create Character");
            System.out.println("2) Manage Inventory");
            System.out.println("3) Update Score");
            System.out.println("4) View Achievements");
            System.out.println("5) Leaderboard");
            System.out.println("6) Edit Profile");
            System.out.println("7) Delete Account");
            System.out.println("8) Logout");
            switch (prompt("Choose: ")) {
                case "1" -> chooseCharacter();
                case "2" -> manageInventory();
                case "3" -> updateScore();
                case "4" -> viewAchievements();
                case "5" -> showLeaderboard();
                case "6" -> editProfile();
                case "7" -> { if (deleteAccount()) inSession = false; }
                case "8" -> { currentPlayer = null; inSession = false; }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    // ---------------- Character Management ----------------

    private void chooseCharacter() {
        try {
            List<game.model.Character> characters = characterDAO.getCharactersByPlayer(currentPlayer.getPlayerId());
            if (characters.isEmpty()) {
                System.out.println("No characters yet.");
            } else {
                System.out.println("Your characters:");
                characters.forEach(System.out::println);
            }
            if (!prompt("Create a new character? (y/n): ").equalsIgnoreCase("y")) return;

            String name = prompt("Character name: ");
            String cls = prompt("Class (Warrior/Mage/Archer): ");
            game.model.Character character = switch (cls.trim().toLowerCase()) {
                case "mage" -> new Mage(currentPlayer.getPlayerId(), name, 80, 15, 5, 1);
                case "archer" -> new Archer(currentPlayer.getPlayerId(), name, 90, 12, 8, 1);
                default -> new Warrior(currentPlayer.getPlayerId(), name, 120, 12, 12, 1);
            };
            characterDAO.addCharacter(character);
            System.out.println("Created: " + character);
            System.out.println(character.attack()); // demonstrates polymorphism immediately
        } catch (SQLException e) {
            handleDbError(e);
        }
    }

    // ---------------- Inventory ----------------

    private void manageInventory() {
        try {
            List<Inventory> items = inventoryDAO.getInventoryByPlayer(currentPlayer.getPlayerId());
            if (items.isEmpty()) {
                System.out.println("Inventory is empty.");
            } else {
                System.out.println("Inventory:");
                items.forEach(System.out::println);
            }
            if (!prompt("Add a weapon? (y/n): ").equalsIgnoreCase("y")) return;

            String weaponType = prompt("Type (Sword/Bow): ");
            String name = prompt("Weapon name: ");
            int damage = Integer.parseInt(prompt("Damage: "));
            int quantity = Integer.parseInt(prompt("Quantity: "));

            Weapon weapon = weaponType.trim().equalsIgnoreCase("bow")
                    ? new Bow(name, damage, quantity)
                    : new Sword(name, damage, quantity);
            System.out.println(weapon.specialEffect());

            Inventory item = new Inventory(currentPlayer.getPlayerId(), weapon.getName(),
                    weapon.getDamage(), weapon.getQuantity());
            inventoryDAO.addItem(item);
            System.out.println("Added to inventory: " + item);
        } catch (SQLException e) {
            handleDbError(e);
        } catch (NumberFormatException e) {
            System.out.println("Damage and quantity must be numbers.");
        }
    }

    // ---------------- Score / Leaderboard ----------------

    private void updateScore() {
        try {
            int score = Integer.parseInt(prompt("Score to submit: "));
            scoreDAO.submitScore(currentPlayer.getPlayerId(), score);
            currentPlayer.gainXp(score / 10); // small XP reward tied to performance
            playerDAO.updatePlayer(currentPlayer);
            System.out.println("Score saved. " + currentPlayer);
        } catch (SQLException e) {
            handleDbError(e);
        } catch (NumberFormatException e) {
            System.out.println("Score must be a number.");
        }
    }

    private void showLeaderboard() {
        try {
            List<String> board = scoreDAO.getLeaderboard();
            System.out.println("=== LEADERBOARD ===");
            if (board.isEmpty()) {
                System.out.println("No scores yet.");
            } else {
                board.forEach(System.out::println);
            }
        } catch (SQLException e) {
            handleDbError(e);
        }
    }

    // ---------------- Achievements ----------------

    private void viewAchievements() {
        try {
            List<Achievement> achievements = achievementDAO.getAchievementsByPlayer(currentPlayer.getPlayerId());
            if (achievements.isEmpty()) {
                System.out.println("No achievements yet.");
            } else {
                achievements.forEach(System.out::println);
            }
        } catch (SQLException e) {
            handleDbError(e);
        }
    }

    // ---------------- Helpers ----------------

    private String prompt(String message) {
        System.out.print(message);
        return scanner.nextLine();
    }

    private void handleDbError(SQLException e) {
        System.out.println("Database error: " + e.getMessage());
    }
}
