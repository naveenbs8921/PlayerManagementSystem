package game.model;

public class Bow extends Weapon {

    public Bow(String name, int damage, int quantity) {
        super(name, damage, quantity);
    }

    @Override
    public String specialEffect() {
        return name + ": has a chance to fire a piercing shot that hits two enemies.";
    }
}
