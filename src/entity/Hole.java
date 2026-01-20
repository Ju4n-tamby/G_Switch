package entity;

import core.GameConfig;
import java.awt.*;

/**
 * Représente un trou dans la plateforme
 */
public class Hole {

    private double x;
    private int width;
    private float wavePhase;

    public Hole(double x, int width) {
        this.x = x;
        this.width = width;
        this.wavePhase = (float) (Math.random() * Math.PI * 2);
    }

    /**
     * Met à jour le trou (déplacement)
     */
    public void update() {
        x -= GameConfig.GAME_SPEED;
        wavePhase += 0.05f;
    }

    /**
     * Vérifie si le trou est hors écran
     */
    public boolean isOffScreen() {
        return x + width < 0;
    }

    /**
     * Dessine le trou (simplifié pour performance)
     */
    public void render(Graphics2D g2d, int groundY, int ceilingY, int platformHeight) {
        // Rendu simplifié - géré par PlatformRenderer
    }

    // Ancien renderVoid supprimé pour performance

    /**
     * Vérifie si un point X est au-dessus du trou
     */
    public boolean isOver(double entityX, double entityWidth) {
        return entityX + entityWidth > x && entityX < x + width;
    }

    // === GETTERS ===

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public int getWidth() { return width; }
}
