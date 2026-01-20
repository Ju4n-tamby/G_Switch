package entity;

/**
 * Direction de la gravité
 */
public enum Gravity {
    DOWN(1),    // Gravité vers le bas
    UP(-1);     // Gravité vers le haut

    private final int direction;

    Gravity(int direction) {
        this.direction = direction;
    }

    public int getDirection() {
        return direction;
    }

    public Gravity opposite() {
        return this == DOWN ? UP : DOWN;
    }
}
