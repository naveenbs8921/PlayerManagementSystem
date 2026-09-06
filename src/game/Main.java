package game;

import game.db.DatabaseConnection;
import game.manager.GameManager;
import game.server.ApiServer;

public class Main {
    public static void main(String[] args) throws Exception {
        try {
            if ("console".equalsIgnoreCase(System.getenv("APP_MODE"))) {
                new GameManager().start();
            } else {
                new ApiServer().start();
            }
        } finally {
            DatabaseConnection.closeConnection();
        }
    }
}
