package graphics;

import core.GameConfig;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Gère le fond animé style Cyberpunk/Synthwave
 */
public class CyberpunkBackground {
    
    // Effet de grille
    private float gridOffset;
    private static final int GRID_SPACING = 60;
    
    // Étoiles/particules de fond
    private List<Star> stars;
    private static final int STAR_COUNT = 30;  // Réduit pour performance
    
    // Lignes de néon flottantes
    private List<NeonLine> neonLines;
    private static final int NEON_LINE_COUNT = 4;  // Réduit pour performance
    
    // Effet de vague
    private float wavePhase;
    
    // Dégradé de fond
    private GradientPaint backgroundGradient;
    
    public CyberpunkBackground() {
        this.gridOffset = 0;
        this.wavePhase = 0;
        initStars();
        initNeonLines();
        createGradient();
    }
    
    private void initStars() {
        stars = new ArrayList<>();
        for (int i = 0; i < STAR_COUNT; i++) {
            stars.add(new Star());
        }
    }
    
    private void initNeonLines() {
        neonLines = new ArrayList<>();
        for (int i = 0; i < NEON_LINE_COUNT; i++) {
            neonLines.add(new NeonLine());
        }
    }
    
    private void createGradient() {
        backgroundGradient = new GradientPaint(
            0, 0, new Color(10, 0, 30),
            0, GameConfig.WINDOW_HEIGHT, new Color(30, 0, 60)
        );
    }
    
    /**
     * Met à jour les animations du fond
     */
    public void update() {
        // Défilement de la grille
        gridOffset += GameConfig.GAME_SPEED * 0.5f;
        if (gridOffset >= GRID_SPACING) {
            gridOffset -= GRID_SPACING;
        }
        
        // Animation des étoiles
        for (Star star : stars) {
            star.update();
        }
        
        // Animation des lignes néon
        for (NeonLine line : neonLines) {
            line.update();
        }
        
        // Phase de vague
        wavePhase += 0.02f;
    }
    
    /**
     * Dessine le fond complet (version optimisée)
     */
    public void render(Graphics2D g2d) {
        // 1. Fond uni (plus rapide que gradient)
        g2d.setColor(new Color(15, 5, 30));
        g2d.fillRect(0, 0, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        
        // 2. Grille simple
        renderGridSimple(g2d);
        
        // 3. Quelques étoiles seulement
        renderStars(g2d);
    }
    
    /**
     * Grille simplifiée pour performance
     */
    private void renderGridSimple(Graphics2D g2d) {
        g2d.setStroke(new BasicStroke(1));
        g2d.setColor(new Color(255, 0, 128, 25));
        
        int topY = GameConfig.CEILING_Y;
        int bottomY = GameConfig.GROUND_Y;
        
        // Quelques lignes verticales seulement
        for (int x = -(int) gridOffset; x < GameConfig.WINDOW_WIDTH + GRID_SPACING; x += GRID_SPACING) {
            g2d.drawLine(x, topY, x, bottomY);
        }
    }
    
    /**
     * Dessine les étoiles
     */
    private void renderStars(Graphics2D g2d) {
        for (Star star : stars) {
            star.render(g2d);
        }
    }
    
    /**
     * Dessine la grille perspective style synthwave
     */
    private void renderGrid(Graphics2D g2d) {
        g2d.setStroke(new BasicStroke(1));
        
        // Zone de jeu (entre plafond et sol)
        int topY = GameConfig.CEILING_Y + GameConfig.PLATFORM_HEIGHT;
        int bottomY = GameConfig.GROUND_Y;
        int midY = (topY + bottomY) / 2;
        
        // Lignes horizontales avec effet de perspective
        for (int i = 0; i < 20; i++) {
            float progress = (float) i / 20;
            int y = (int) (midY + (bottomY - midY) * progress * progress);
            int alpha = (int) (30 + progress * 50);
            g2d.setColor(new Color(255, 0, 128, alpha));
            g2d.drawLine(0, y, GameConfig.WINDOW_WIDTH, y);
        }
        
        // Lignes verticales avec défilement
        int lineAlpha = 40;
        g2d.setColor(new Color(0, 255, 255, lineAlpha));
        
        for (int x = -(int) gridOffset; x < GameConfig.WINDOW_WIDTH + GRID_SPACING; x += GRID_SPACING) {
            // Ligne droite dans la zone de jeu
            g2d.drawLine(x, topY, x, bottomY);
        }
    }
    
    /**
     * Dessine les lignes de néon flottantes
     */
    private void renderNeonLines(Graphics2D g2d) {
        for (NeonLine line : neonLines) {
            line.render(g2d);
        }
    }
    
    /**
     * Dessine l'effet de brume
     */
    private void renderMist(Graphics2D g2d) {
        int mistHeight = 100;
        for (int i = 0; i < mistHeight; i++) {
            int alpha = (int) ((1 - (float) i / mistHeight) * 30);
            g2d.setColor(new Color(180, 0, 255, alpha));
            g2d.drawLine(0, GameConfig.WINDOW_HEIGHT - i, 
                        GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT - i);
        }
    }
    
    // === CLASSE INTERNE STAR ===
    
    private class Star {
        double x, y;
        double speed;
        int size;
        int alpha;
        float twinklePhase;
        
        Star() {
            reset();
            x = Math.random() * GameConfig.WINDOW_WIDTH;
        }
        
        void reset() {
            x = GameConfig.WINDOW_WIDTH + Math.random() * 100;
            y = Math.random() * GameConfig.WINDOW_HEIGHT;
            speed = 0.5 + Math.random() * 2;
            size = 1 + (int) (Math.random() * 3);
            alpha = 50 + (int) (Math.random() * 150);
            twinklePhase = (float) (Math.random() * Math.PI * 2);
        }
        
        void update() {
            x -= speed;
            twinklePhase += 0.1f;
            
            if (x < -10) {
                reset();
            }
        }
        
        void render(Graphics2D g2d) {
            int twinkle = (int) (Math.sin(twinklePhase) * 30 + alpha);
            twinkle = Math.max(0, Math.min(255, twinkle));
            
            g2d.setColor(new Color(255, 255, 255, twinkle));
            g2d.fillOval((int) x, (int) y, size, size);
            
            // Halo pour les grandes étoiles
            if (size >= 2) {
                g2d.setColor(new Color(200, 200, 255, twinkle / 4));
                g2d.fillOval((int) x - 1, (int) y - 1, size + 2, size + 2);
            }
        }
    }
    
    // === CLASSE INTERNE NEONLINE ===
    
    private class NeonLine {
        double x, y;
        double speed;
        int length;
        Color color;
        float alpha;
        
        NeonLine() {
            reset();
            x = Math.random() * GameConfig.WINDOW_WIDTH;
        }
        
        void reset() {
            x = GameConfig.WINDOW_WIDTH + Math.random() * 200;
            y = GameConfig.CEILING_Y + GameConfig.PLATFORM_HEIGHT + 
                Math.random() * (GameConfig.GROUND_Y - GameConfig.CEILING_Y - GameConfig.PLATFORM_HEIGHT * 2);
            speed = 2 + Math.random() * 4;
            length = 50 + (int) (Math.random() * 150);
            
            // Couleur aléatoire cyberpunk
            Color[] colors = {GameConfig.NEON_CYAN, GameConfig.NEON_PINK, 
                             GameConfig.NEON_PURPLE, GameConfig.NEON_BLUE};
            color = colors[(int) (Math.random() * colors.length)];
            alpha = 0.3f + (float) Math.random() * 0.5f;
        }
        
        void update() {
            x -= speed;
            
            if (x + length < 0) {
                reset();
            }
        }
        
        void render(Graphics2D g2d) {
            // Ligne principale
            g2d.setColor(new Color(color.getRed(), color.getGreen(), 
                                  color.getBlue(), (int) (alpha * 255)));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine((int) x, (int) y, (int) x + length, (int) y);
            
            // Glow
            g2d.setColor(new Color(color.getRed(), color.getGreen(), 
                                  color.getBlue(), (int) (alpha * 100)));
            g2d.setStroke(new BasicStroke(4));
            g2d.drawLine((int) x, (int) y, (int) x + length, (int) y);
        }
    }
}
