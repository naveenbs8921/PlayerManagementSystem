package game.model;

public class Archer extends Character {

    public Archer(int playerId, String characterName, int health, int attack, int defense, int level) {
        super(playerId, characterName, health, attack, defense, level);
    }

    public Archer(int characterId, int playerId, String characterName,
                  int health, int attack, int defense, int level) {
        super(characterId, playerId, characterName, health, attack, defense, level);
    }

    @Override
    public String attack() {
        int damage = calculateAttackDamage();
        return characterName + " looses two rapid arrows for " + damage + " piercing damage!";
    }

    @Override
    public String defend() {
        return characterName + " slips into the shadows, evading incoming strikes!";
    }

    @Override
    public String specialAbility() {
        int damage = calculateSpecialDamage();
        return characterName + " fires Piercing Volley! A storm of razor arrows hitting for " + damage + " critical damage!";
    }

    @Override
    public int calculateAttackDamage() {
        return attack + 4;
    }

    @Override
    public int calculateSpecialDamage() {
        return attack * 2 + 14;
    }

    @Override
    public int calculateDefenseValue() {
        return defense * 2;
    }

    @Override
    public String getCharacterClass() {
        return "Archer";
    }
}
