package game.model;

/**
 * Abstract base for all playable classes.
 * Exposes only the essentials (attack, take damage, stat access);
 * concrete subclasses decide *how* attack() behaves.
 */
public abstract class Character {

    protected int characterId;
    protected int playerId;
    protected String characterName;
    protected int health;
    protected int attack;
    protected int defense;
    protected int level;

    protected Character(int playerId, String characterName,
                         int health, int attack, int defense, int level) {
        this.playerId = playerId;
        this.characterName = characterName;
        this.health = health;
        this.attack = attack;
        this.defense = defense;
        this.level = level;
    }

    protected Character(int characterId, int playerId, String characterName,
                         int health, int attack, int defense, int level) {
        this(playerId, characterName, health, attack, defense, level);
        this.characterId = characterId;
    }

    /** Polymorphic behavior — every subclass attacks differently. */
    public abstract String attack();

    /** Defensive stance action. */
    public abstract String defend();

    /** Unique class special ability. */
    public abstract String specialAbility();

    /** Concrete base attack damage calculation. */
    public abstract int calculateAttackDamage();

    /** Concrete special attack damage calculation. */
    public abstract int calculateSpecialDamage();

    /** Active defense mitigation value. */
    public int calculateDefenseValue() {
        return defense;
    }

    /** Used to populate the Characters.Class column. */
    public abstract String getCharacterClass();

    public void takeDamage(int amount) {
        int mitigated = Math.max(0, amount - defense);
        health = Math.max(0, health - mitigated);
    }

    public int getCharacterId() { return characterId; }
    public void setCharacterId(int characterId) { this.characterId = characterId; }

    public int getPlayerId() { return playerId; }
    public void setPlayerId(int playerId) { this.playerId = playerId; }

    public String getCharacterName() { return characterName; }
    public void setCharacterName(String characterName) { this.characterName = characterName; }

    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }

    public int getAttackStat() { return attack; }
    public void setAttackStat(int attack) { this.attack = attack; }

    public int getDefense() { return defense; }
    public void setDefense(int defense) { this.defense = defense; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    @Override
    public String toString() {
        return String.format("#%d %s [%s] Lv.%d | HP:%d ATK:%d DEF:%d",
                characterId, characterName, getCharacterClass(), level, health, attack, defense);
    }
}
