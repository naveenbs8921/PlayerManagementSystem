package game.model;

public class Achievement {

    private int achievementId;
    private int playerId;
    private String title;
    private String description;
    private boolean unlocked;

    public Achievement(int playerId, String title, String description, boolean unlocked) {
        this.playerId = playerId;
        this.title = title;
        this.description = description;
        this.unlocked = unlocked;
    }

    public Achievement(int achievementId, int playerId, String title, String description, boolean unlocked) {
        this(playerId, title, description, unlocked);
        this.achievementId = achievementId;
    }

    public int getAchievementId() { return achievementId; }
    public void setAchievementId(int achievementId) { this.achievementId = achievementId; }

    public int getPlayerId() { return playerId; }
    public void setPlayerId(int playerId) { this.playerId = playerId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s", unlocked ? "X" : " ", title, description);
    }
}
