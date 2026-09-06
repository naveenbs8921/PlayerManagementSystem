package game.model;

public class Sword extends Weapon {

    public Sword(String name, int damage, int quantity) {
        super(name, damage, quantity);
    }

    @Override
    public String specialEffect() {
        return name + ": a well-timed swing can cause bleed damage over time.";
    }
}
