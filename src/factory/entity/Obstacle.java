package factory.entity;

import core.GameConfig;
import java.awt.*;

/**
 * Représente un obstacle sur la piste
 */
public class Obstacle {

    public enum ObstacleType {
        GROUND,     // Sur le sol
        CEILING,    // Sur le plafond
        SPIKE,      // Pointe
        BLOCK       // Bloc simple
    }

    private double x;
    private double y;
    private int width;
    private int height;
    private ObstacleType type;
    private Color glowColor;
    private float pulsePhase;

    public Obstacle(double x, double y, int width, int height, ObstacleType type) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.type = type;
        this.glowColor = type == ObstacleType.CEILING ?
                        GameConfig.NEON_PURPLE : GameConfig.NEON_PINK;
        this.pulsePhase = (float) (Math.random() * Math.PI * 2);
    }

    /**
     * Constructeur simplifié pour le réseau
     */
    public Obstacle(int x, int y, int width, int height) {
        this(x, y, width, height, y < GameConfig.WINDOW_HEIGHT / 2 ?
             ObstacleType.CEILING : ObstacleType.GROUND);
    }

    /**
     * Met à jour l'obstacle (déplacement)
     */
    public void update() {
        x -= GameConfig.GAME_SPEED;
        pulsePhase += 0.1f;
    }

    /**
     * Vérifie si l'obstacle est hors écran
     */
    public boolean isOffScreen() {
        return x + width < 0;
    }

    /**
     * Dessine l'obstacle (simplifié pour performance)
     */
    public void render(Graphics2D g2d) {
        int px = (int) x;
        int py = (int) y;

        // Corps simple
        g2d.setColor(glowColor);
        g2d.fillRect(px, py, width, height);

        // Contour
        g2d.setColor(glowColor.brighter());
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(px, py, width, height);
    }

    /**
     * Rectangle de collision
     */
    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, width, height);
    }

    // === GETTERS ===

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public ObstacleType getType() { return type; }
}
