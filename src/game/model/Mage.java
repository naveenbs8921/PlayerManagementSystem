package game.model;

public class Mage extends Character {

    public Mage(int playerId, String characterName, int health, int attack, int defense, int level) {
        super(playerId, characterName, health, attack, defense, level);
    }

    public Mage(int characterId, int playerId, String characterName,
                int health, int attack, int defense, int level) {
        super(characterId, playerId, characterName, health, attack, defense, level);
    }

    @Override
    public String attack() {
        int damage = calculateAttackDamage();
        return characterName + " casts a searing fireball for " + damage + " magic damage!";
    }

    @Override
    public String defend() {
        return characterName + " weaves a shimmering Mana Barrier, repelling kinetic force!";
    }

    @Override
    public String specialAbility() {
        int damage = calculateSpecialDamage();
        return characterName + " channels Arcane Nova! Pure mystical lightning surges for " + damage + " damage!";
    }

    @Override
    public int calculateAttackDamage() {
        return attack + 10;
    }

    @Override
    public int calculateSpecialDamage() {
        return attack * 2 + 18;
    }

    @Override
    public int calculateDefenseValue() {
        return defense + 12;
    }

    @Override
    public String getCharacterClass() {
        return "Mage";
    }
}
