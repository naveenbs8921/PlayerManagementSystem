package game.model;

public abstract class Weapon {

    protected String name;
    protected int damage;
    protected int quantity;

    protected Weapon(String name, int damage, int quantity) {
        this.name = name;
        this.damage = damage;
        this.quantity = quantity;
    }

    /** Each weapon type describes its own special effect (polymorphism). */
    public abstract String specialEffect();

    public String getName() { return name; }
    public int getDamage() { return damage; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
