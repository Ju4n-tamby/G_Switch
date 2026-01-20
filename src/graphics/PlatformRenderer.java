package graphics;

import core.GameConfig;
import java.awt.*;

/**
 * Rendu des plateformes avec style cyberpunk
 */
public class PlatformRenderer {
    
    private float pulsePhase;
    private float scrollOffset;
    
    public PlatformRenderer() {
        this.pulsePhase = 0;
        this.scrollOffset = 0;
    }
    
    /**
     * Met à jour les animations
     */
    public void update() {
        pulsePhase += 0.05f;
        scrollOffset += GameConfig.GAME_SPEED;
        if (scrollOffset >= 40) {
            scrollOffset -= 40;
        }
    }
    
    /**
     * Dessine le sol
     */
    public void renderGround(Graphics2D g2d) {
        int y = GameConfig.GROUND_Y;
        int height = GameConfig.WINDOW_HEIGHT - y;
        
        renderPlatform(g2d, 0, y, GameConfig.WINDOW_WIDTH, height, false);
    }
    
    /**
     * Dessine le plafond
     */
    public void renderCeiling(Graphics2D g2d) {
        int height = GameConfig.CEILING_Y;
        
        renderPlatform(g2d, 0, 0, GameConfig.WINDOW_WIDTH, height, true);
    }
    
    /**
     * Dessine une plateforme avec effet néon (simplifié pour performance)
     */
    private void renderPlatform(Graphics2D g2d, int x, int y, int width, int height, boolean isCeiling) {
        // Corps principal - couleur solide au lieu de gradient
        g2d.setColor(new Color(30, 30, 60));
        g2d.fillRect(x, y, width, height);
        
        // Bord lumineux simple
        int edgeY = isCeiling ? y + height - 3 : y;
        g2d.setColor(GameConfig.NEON_CYAN);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(x, edgeY, x + width, edgeY);
    }
    
    // Pattern circuit désactivé pour performance
    
    /**
     * Dessine les bords des trous (simplifié)
     */
    public void renderHoleEdges(Graphics2D g2d, double holeX, int holeWidth, boolean isGround) {
        int edgeY = isGround ? GameConfig.GROUND_Y : 0;
        int height = isGround ? GameConfig.WINDOW_HEIGHT - GameConfig.GROUND_Y : GameConfig.CEILING_Y;
        
        // Vide noir
        g2d.setColor(Color.BLACK);
        g2d.fillRect((int) holeX, edgeY, holeWidth, height);
        
        // Bords violets simples
        g2d.setColor(GameConfig.NEON_PURPLE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine((int) holeX, edgeY, (int) holeX, edgeY + height);
        g2d.drawLine((int) holeX + holeWidth, edgeY, (int) holeX + holeWidth, edgeY + height);
    }
}
