package game.model;

/** Maps directly to one row of the Inventory table. */
public class Inventory {

    private int itemId;
    private int playerId;
    private String weaponName;
    private int damage;
    private int quantity;

    public Inventory(int playerId, String weaponName, int damage, int quantity) {
        this.playerId = playerId;
        this.weaponName = weaponName;
        this.damage = damage;
        this.quantity = quantity;
    }

    public Inventory(int itemId, int playerId, String weaponName, int damage, int quantity) {
        this(playerId, weaponName, damage, quantity);
        this.itemId = itemId;
    }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public int getPlayerId() { return playerId; }
    public void setPlayerId(int playerId) { this.playerId = playerId; }

    public String getWeaponName() { return weaponName; }
    public void setWeaponName(String weaponName) { this.weaponName = weaponName; }

    public int getDamage() { return damage; }
    public void setDamage(int damage) { this.damage = damage; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    @Override
    public String toString() {
        return String.format("#%d %s | DMG:%d  x%d", itemId, weaponName, damage, quantity);
    }
}
