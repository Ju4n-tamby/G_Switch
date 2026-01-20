package ui.panels;

import core.GameConfig;
import core.GameEngine;
import core.GameState;
import core.PlayerConfig;
import ui.components.NeonButton;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel pour sélectionner le nombre de joueurs et leurs touches
 */
public class PlayerSelectionPanel extends JPanel {
    
    private GameEngine engine;
    private Timer animationTimer;
    private int numberOfPlayers;
    private List<PlayerConfig> playerConfigs;
    private List<NeonButton> playerButtons;
    private NeonButton playButton;
    private NeonButton backButton;
    
    // Animation
    private float titleGlow;
    private float titlePhase;
    private List<MenuParticle> particles;
    private float gridOffset;
    
    public PlayerSelectionPanel(GameEngine engine) {
        this.engine = engine;
        this.numberOfPlayers = 1;
        this.playerConfigs = new ArrayList<>();
        this.playerButtons = new ArrayList<>();
        this.particles = new ArrayList<>();
        
        setPreferredSize(new Dimension(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT));
        setBackground(GameConfig.DARK_BG);
        setFocusable(true);
        setDoubleBuffered(true);
        
        initButtons();
        initParticles();
        setupMouseListeners();
        
        // Timer d'animation optimisé
        animationTimer = new Timer(16, e -> {
            updateAnimations();
            repaint();
        });
        animationTimer.setCoalesce(true);
        animationTimer.start();
    }
    
    private void initButtons() {
        playerButtons.clear();
        
        int centerX = GameConfig.WINDOW_WIDTH / 2;
        int buttonWidth = 280;
        int buttonHeight = 55;
        int startY = 280;
        int spacing = 70;
        
        // Boutons pour sélectionner le nombre de joueurs (1-4)
        for (int i = 1; i <= 4; i++) {
            final int playerCount = i;
            NeonButton btn = new NeonButton(
                i + " JOUEUR" + (i > 1 ? "S" : ""),
                centerX - buttonWidth / 2,
                startY + (i - 1) * spacing,
                buttonWidth,
                buttonHeight,
                (i == numberOfPlayers) ? GameConfig.NEON_GREEN : GameConfig.NEON_CYAN
            );
            btn.setOnClick(() -> {
                numberOfPlayers = playerCount;
                updatePlayerConfigs();
                initButtons(); // Recréer les boutons pour mettre à jour les couleurs
            });
            playerButtons.add(btn);
        }
        
        // Bouton JOUER
        playButton = new NeonButton(
            "JOUER",
            centerX - buttonWidth / 2,
            startY + 350,
            buttonWidth,
            buttonHeight,
            GameConfig.NEON_GREEN
        );
        playButton.setOnClick(() -> {
            engine.setPlayerConfigs(playerConfigs);
            engine.setState(GameState.PLAYING);
        });
        
        // Bouton RETOUR
        backButton = new NeonButton(
            "RETOUR",
            centerX - buttonWidth / 2,
            startY + 420,
            buttonWidth,
            buttonHeight,
            GameConfig.NEON_PINK
        );
        backButton.setOnClick(() -> engine.setState(GameState.MENU));
        
        updatePlayerConfigs();
    }
    
    private void updatePlayerConfigs() {
        playerConfigs.clear();
        for (int i = 1; i <= numberOfPlayers; i++) {
            playerConfigs.add(PlayerConfig.createWithScheme(i));
        }
    }
    
    private void initParticles() {
        particles.clear();
        for (int i = 0; i < 50; i++) {
            particles.add(new MenuParticle());
        }
    }
    
    private void setupMouseListeners() {
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                for (NeonButton btn : playerButtons) {
                    btn.setHovered(btn.contains(e.getX(), e.getY()));
                }
                playButton.setHovered(playButton.contains(e.getX(), e.getY()));
                backButton.setHovered(backButton.contains(e.getX(), e.getY()));
                repaint();
            }
        });
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                for (NeonButton btn : playerButtons) {
                    if (btn.contains(e.getX(), e.getY())) btn.setPressed(true);
                }
                if (playButton.contains(e.getX(), e.getY())) playButton.setPressed(true);
                if (backButton.contains(e.getX(), e.getY())) backButton.setPressed(true);
                repaint();
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                for (NeonButton btn : playerButtons) {
                    if (btn.isHovered() && btn.contains(e.getX(), e.getY())) {
                        btn.click();
                    }
                    btn.setPressed(false);
                }
                if (playButton.isHovered() && playButton.contains(e.getX(), e.getY())) {
                    playButton.click();
                }
                playButton.setPressed(false);
                
                if (backButton.isHovered() && backButton.contains(e.getX(), e.getY())) {
                    backButton.click();
                }
                backButton.setPressed(false);
                
                repaint();
            }
        });
    }
    
    private void updateAnimations() {
        // Animation du titre
        titlePhase += 0.05f;
        titleGlow = (float) (Math.sin(titlePhase) * 0.3 + 0.7);
        
        // Animation de la grille
        gridOffset += 0.5f;
        if (gridOffset >= 50) {
            gridOffset -= 50;
        }
        
        // Mise à jour des particules
        for (MenuParticle p : particles) {
            p.update();
        }
        
        // Mise à jour des boutons
        for (NeonButton btn : playerButtons) {
            btn.update();
        }
        playButton.update();
        backButton.update();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Fond gradient
        GradientPaint gradient = new GradientPaint(
            0, 0, new Color(0, 0, 20),
            GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT, new Color(10, 0, 30)
        );
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        
        // Grille animée
        g2d.setColor(new Color(0, 255, 200, 5));
        g2d.setStroke(new BasicStroke(1));
        for (float x = -gridOffset; x < GameConfig.WINDOW_WIDTH; x += 50) {
            g2d.drawLine((int) x, 0, (int) x, GameConfig.WINDOW_HEIGHT);
        }
        for (float y = -gridOffset; y < GameConfig.WINDOW_HEIGHT; y += 50) {
            g2d.drawLine(0, (int) y, GameConfig.WINDOW_WIDTH, (int) y);
        }
        
        // Particules
        for (MenuParticle p : particles) {
            p.render(g2d);
        }
        
        // Titre
        g2d.setFont(new Font("Arial", Font.BOLD, 56));
        String title = "SÉLECTION JOUEURS";
        FontMetrics fm = g2d.getFontMetrics();
        int x = (GameConfig.WINDOW_WIDTH - fm.stringWidth(title)) / 2;
        
        // Glow du titre
        for (int i = 20; i > 0; i--) {
            g2d.setColor(new Color(0, 255, 200, (int) (titleGlow * (20 - i) * 3)));
            g2d.drawString(title, x, 100);
        }
        g2d.setColor(GameConfig.NEON_CYAN);
        g2d.drawString(title, x, 100);
        
        // Afficher la sélection actuelle
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        g2d.setColor(new Color(150, 150, 150));
        g2d.drawString("Nombre de joueurs sélectionnés: " + numberOfPlayers, 
                      GameConfig.WINDOW_WIDTH / 2 - 180, 200);
        
        // Afficher les touches pour les joueurs sélectionnés
        int configY = 600;
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        for (PlayerConfig config : playerConfigs) {
            g2d.setColor(config.getPlayerColor());
            g2d.drawString(config.toString(), 50, configY);
            configY += 25;
        }
        
        // Boutons joueurs
        for (NeonButton btn : playerButtons) {
            btn.render(g2d);
        }
        
        // Boutons JOUER et RETOUR
        playButton.render(g2d);
        backButton.render(g2d);
    }
    
    public List<PlayerConfig> getPlayerConfigs() {
        return playerConfigs;
    }
    
    /**
     * Classe interne pour les particules de fond
     */
    private class MenuParticle {
        double x, y;
        double speed;
        int size;
        Color color;
        float alpha;
        
        MenuParticle() {
            reset();
            y = Math.random() * GameConfig.WINDOW_HEIGHT;
        }
        
        void reset() {
            x = Math.random() * GameConfig.WINDOW_WIDTH;
            y = GameConfig.WINDOW_HEIGHT + Math.random() * 50;
            speed = 0.5 + Math.random() * 2;
            size = 2 + (int) (Math.random() * 4);
            
            Color[] colors = {GameConfig.NEON_CYAN, GameConfig.NEON_PINK, 
                             GameConfig.NEON_PURPLE, GameConfig.NEON_BLUE};
            color = colors[(int) (Math.random() * colors.length)];
            alpha = 0.2f + (float) Math.random() * 0.5f;
        }
        
        void update() {
            y -= speed;
            x += Math.sin(y * 0.02) * 0.5;
            
            if (y < -10) {
                reset();
            }
        }
        
        void render(Graphics2D g2d) {
            g2d.setColor(new Color(color.getRed(), color.getGreen(),
                                  color.getBlue(), (int) (alpha * 255)));
            g2d.fillOval((int) x, (int) y, size, size);
            
            // Petit glow
            g2d.setColor(new Color(color.getRed(), color.getGreen(),
                                  color.getBlue(), (int) (alpha * 100)));
            g2d.fillOval((int) x - 1, (int) y - 1, size + 2, size + 2);
        }
    }
}
