package game.model;

/** Encapsulated player account — all fields private, accessed only via getters/setters. */
public class Player {

    private int playerId;
    private String name;
    private String email;
    private String password;
    private int level;
    private int xp;
    private int coins;

    public Player(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.level = 1;
        this.xp = 0;
        this.coins = 100;
    }

    public Player(int playerId, String name, String email, String password,
                   int level, int xp, int coins) {
        this.playerId = playerId;
        this.name = name;
        this.email = email;
        this.password = password;
        this.level = level;
        this.xp = xp;
        this.coins = coins;
    }

    public int getPlayerId() { return playerId; }
    public void setPlayerId(int playerId) { this.playerId = playerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }

    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }

    /** Adds XP and auto-levels up every 100 XP. */
    public void gainXp(int amount) {
        this.xp += amount;
        while (this.xp >= 100) {
            this.xp -= 100;
            this.level++;
        }
    }

    @Override
    public String toString() {
        return String.format("#%d %s <%s> | Lv.%d  XP:%d  Coins:%d",
                playerId, name, email, level, xp, coins);
    }
}
