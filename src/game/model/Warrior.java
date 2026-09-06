package game.model;

public class Warrior extends Character {

    public Warrior(int playerId, String characterName, int health, int attack, int defense, int level) {
        super(playerId, characterName, health, attack, defense, level);
    }

    public Warrior(int characterId, int playerId, String characterName,
                    int health, int attack, int defense, int level) {
        super(characterId, playerId, characterName, health, attack, defense, level);
    }

    @Override
    public String attack() {
        int damage = calculateAttackDamage();
        return characterName + " swings a greatsword for " + damage + " damage!";
    }

    @Override
    public String defend() {
        return characterName + " raises an iron tower shield, bracing for the incoming blow!";
    }

    @Override
    public String specialAbility() {
        int damage = calculateSpecialDamage();
        return characterName + " unleashes Earth Shatter! A seismic slam dealing " + damage + " heavy damage!";
    }

    @Override
    public int calculateAttackDamage() {
        return attack + 5;
    }

    @Override
    public int calculateSpecialDamage() {
        return attack * 2 + 10;
    }

    @Override
    public int calculateDefenseValue() {
        return defense * 2 + 2;
    }

    @Override
    public String getCharacterClass() {
        return "Warrior";
    }
}
