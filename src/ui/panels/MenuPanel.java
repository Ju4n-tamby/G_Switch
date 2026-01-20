package ui.panels;

import core.GameConfig;
import core.GameEngine;
import core.GameState;
import core.PlayerConfig;
import graphics.ResourceManager;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import ui.components.NeonButton;

/**
 * Écran d'accueil avec style néon minimaliste
 */
public class MenuPanel extends JPanel {

    private GameEngine engine;
    private Timer animationTimer;
    private ResourceManager resources;

    // Boutons
    private List<NeonButton> buttons;
    private NeonButton soloButton;
    private NeonButton multiLocalButton;
    private NeonButton networkButton;
    private NeonButton infoButton;
    private NeonButton quitButton;

    // Animations
    private float titleGlow;
    private float titlePhase;
    private List<FloatingParticle> particles;
    private float pulsePhase;

    public MenuPanel(GameEngine engine) {
        this.engine = engine;
        this.particles = new ArrayList<>();
        this.resources = ResourceManager.getInstance();

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
        buttons = new ArrayList<>();

        int buttonWidth = 320;
        int buttonHeight = 55;
        int centerX = GameConfig.WINDOW_WIDTH / 2 - buttonWidth / 2;
        int startY = 400;
        int spacing = 68;

        // Bouton Solo
        soloButton = new NeonButton("▶  JOUER SEUL", centerX, startY, buttonWidth, buttonHeight, GameConfig.NEON_CYAN);
        soloButton.setOnClick(() -> {
            List<PlayerConfig> soloConfigs = new ArrayList<>();
            soloConfigs.add(PlayerConfig.createWithScheme(1));
            engine.setPlayerConfigs(soloConfigs);
            engine.setState(GameState.PLAYING);
        });

        // Bouton Multijoueur Local
        multiLocalButton = new NeonButton("👥  MULTIJOUEUR LOCAL", centerX, startY + spacing, buttonWidth, buttonHeight, GameConfig.NEON_PURPLE);
        multiLocalButton.setOnClick(() -> engine.setState(GameState.PLAYER_SELECTION));

        // Bouton Réseau
        networkButton = new NeonButton("🌐  JOUER EN RÉSEAU", centerX, startY + spacing * 2, buttonWidth, buttonHeight, GameConfig.NEON_BLUE);
        networkButton.setOnClick(() -> engine.setState(GameState.NETWORK));

        // Bouton Info
        infoButton = new NeonButton("ℹ  À PROPOS", centerX, startY + spacing * 3, buttonWidth, buttonHeight, GameConfig.NEON_ORANGE);
        infoButton.setOnClick(() -> engine.setState(GameState.INFO));

        // Bouton Quitter
        quitButton = new NeonButton("✕  QUITTER", centerX, startY + spacing * 4, buttonWidth, buttonHeight, GameConfig.NEON_PINK);
        quitButton.setOnClick(() -> engine.exitGame());

        buttons.add(soloButton);
        buttons.add(multiLocalButton);
        buttons.add(networkButton);
        buttons.add(infoButton);
        buttons.add(quitButton);
    }

    private void initParticles() {
        for (int i = 0; i < 30; i++) {
            particles.add(new FloatingParticle());
        }
    }

    private void setupMouseListeners() {
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                for (NeonButton button : buttons) {
                    button.setHovered(button.contains(e.getX(), e.getY()));
                }
                repaint();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                for (NeonButton button : buttons) {
                    if (button.contains(e.getX(), e.getY())) {
                        button.setPressed(true);
                    }
                }
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                for (NeonButton button : buttons) {
                    if (button.isHovered() && button.contains(e.getX(), e.getY())) {
                        button.click();
                    }
                    button.setPressed(false);
                }
                repaint();
            }
        });
    }

    private void updateAnimations() {
        titlePhase += 0.03f;
        titleGlow = (float) (Math.sin(titlePhase) * 0.3 + 0.7);
        pulsePhase += 0.02f;

        for (FloatingParticle p : particles) {
            p.update();
        }

        for (NeonButton button : buttons) {
            button.update();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        renderBackground(g2d);
        renderParticles(g2d);
        renderTitle(g2d);

        for (NeonButton button : buttons) {
            button.render(g2d);
        }

        renderFooter(g2d);
    }

    private void renderBackground(Graphics2D g2d) {
        // Utiliser l'image de fond si disponible
        if (resources.hasMenuBackground()) {
            resources.drawMenuBackground(g2d, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        } else {
            // Fallback: fond noir profond
            g2d.setColor(GameConfig.DARK_BG);
            g2d.fillRect(0, 0, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        }

        // Grille subtile par-dessus
        g2d.setColor(GameConfig.GRID_COLOR);
        g2d.setStroke(new BasicStroke(1));
        for (int x = 0; x < GameConfig.WINDOW_WIDTH; x += 60) {
            g2d.drawLine(x, 0, x, GameConfig.WINDOW_HEIGHT);
        }
        for (int y = 0; y < GameConfig.WINDOW_HEIGHT; y += 60) {
            g2d.drawLine(0, y, GameConfig.WINDOW_WIDTH, y);
        }

        // Lueur centrale douce
        float pulse = (float) (Math.sin(pulsePhase) * 0.3 + 0.7);
        RadialGradientPaint radial = new RadialGradientPaint(
                GameConfig.WINDOW_WIDTH / 2f, 250,
                400,
                new float[]{0f, 0.5f, 1f},
                new Color[]{
                    new Color(0, 255, 255, (int) (20 * pulse)),
                    new Color(138, 43, 226, (int) (12 * pulse)),
                    new Color(0, 0, 0, 0)
                }
        );
        g2d.setPaint(radial);
        g2d.fillRect(0, 0, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
    }

    private void renderParticles(Graphics2D g2d) {
        for (FloatingParticle p : particles) {
            p.render(g2d);
        }
    }

    private void renderTitle(Graphics2D g2d) {
        int centerX = GameConfig.WINDOW_WIDTH / 2;

        // Titre principal - VOID RUNNER
        Font titleFont = new Font("Arial", Font.BOLD, 82);
        g2d.setFont(titleFont);
        FontMetrics fm = g2d.getFontMetrics();
        String title = GameConfig.GAME_TITLE;
        int titleX = centerX - fm.stringWidth(title) / 2;
        int titleY = 180;

        // Glow du titre
        for (int i = 15; i > 0; i--) {
            int alpha = (int) ((15 - i) * 8 * titleGlow);
            g2d.setColor(new Color(0, 255, 255, Math.min(alpha, 255)));
            g2d.drawString(title, titleX, titleY);
        }

        // Texte principal avec dégradé
        GradientPaint titleGradient = new GradientPaint(
                titleX, titleY - 50, GameConfig.NEON_CYAN,
                titleX + fm.stringWidth(title), titleY, new Color(138, 43, 226)
        );
        g2d.setPaint(titleGradient);
        g2d.drawString(title, titleX, titleY);

        // Highlight subtil
        g2d.setColor(new Color(255, 255, 255, (int) (80 * titleGlow)));
        g2d.drawString(title, titleX, titleY - 2);

        // Sous-titre
        Font subtitleFont = new Font("Arial", Font.PLAIN, 20);
        g2d.setFont(subtitleFont);
        fm = g2d.getFontMetrics();
        String subtitle = GameConfig.GAME_SUBTITLE;
        int subtitleX = centerX - fm.stringWidth(subtitle) / 2;

        g2d.setColor(GameConfig.TEXT_SECONDARY);
        g2d.drawString(subtitle, subtitleX, titleY + 45);

        // Lignes décoratives minimalistes
        int lineY = titleY + 80;
        int lineWidth = 150;

        g2d.setStroke(new BasicStroke(1.5f));

        // Ligne gauche avec dégradé
        GradientPaint lineGrad = new GradientPaint(
                centerX - lineWidth - 60, lineY, new Color(0, 0, 0, 0),
                centerX - 60, lineY, GameConfig.NEON_CYAN
        );
        g2d.setPaint(lineGrad);
        g2d.drawLine(centerX - lineWidth - 60, lineY, centerX - 60, lineY);

        // Ligne droite avec dégradé
        lineGrad = new GradientPaint(
                centerX + 60, lineY, GameConfig.NEON_CYAN,
                centerX + lineWidth + 60, lineY, new Color(0, 0, 0, 0)
        );
        g2d.setPaint(lineGrad);
        g2d.drawLine(centerX + 60, lineY, centerX + lineWidth + 60, lineY);

        // Point central lumineux
        g2d.setColor(GameConfig.NEON_CYAN);
        g2d.fillOval(centerX - 4, lineY - 4, 8, 8);
        g2d.setColor(new Color(0, 255, 255, 100));
        g2d.fillOval(centerX - 8, lineY - 8, 16, 16);
    }

    private void renderFooter(Graphics2D g2d) {
        g2d.setFont(new Font("Arial", Font.PLAIN, 13));

        // Version
        g2d.setColor(GameConfig.TEXT_SECONDARY);
        g2d.drawString(GameConfig.GAME_VERSION, 30, GameConfig.WINDOW_HEIGHT - 25);

        // Instructions
        String instructions = "Utilisez la souris pour naviguer";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(instructions, (GameConfig.WINDOW_WIDTH - fm.stringWidth(instructions)) / 2,
                GameConfig.WINDOW_HEIGHT - 25);

        // Copyright
        String copyright = "© 2026 VOID RUNNER";
        g2d.drawString(copyright, GameConfig.WINDOW_WIDTH - fm.stringWidth(copyright) - 30,
                GameConfig.WINDOW_HEIGHT - 25);
    }

    // === CLASSE INTERNE - PARTICULE FLOTTANTE ===
    private class FloatingParticle {

        double x, y;
        double vx, vy;
        int size;
        Color color;
        float alpha;

        FloatingParticle() {
            reset();
            x = Math.random() * GameConfig.WINDOW_WIDTH;
            y = Math.random() * GameConfig.WINDOW_HEIGHT;
        }

        void reset() {
            x = Math.random() * GameConfig.WINDOW_WIDTH;
            y = GameConfig.WINDOW_HEIGHT + 10;
            vx = (Math.random() - 0.5) * 0.5;
            vy = -0.3 - Math.random() * 0.8;
            size = 2 + (int) (Math.random() * 3);

            Color[] colors = {GameConfig.NEON_CYAN, GameConfig.NEON_PURPLE};
            color = colors[(int) (Math.random() * colors.length)];
            alpha = 0.15f + (float) Math.random() * 0.25f;
        }

        void update() {
            x += vx;
            y += vy;

            // Léger mouvement ondulant
            x += Math.sin(y * 0.01) * 0.3;

            if (y < -20 || x < -20 || x > GameConfig.WINDOW_WIDTH + 20) {
                reset();
            }
        }

        void render(Graphics2D g2d) {
            int a = (int) (alpha * 255);
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), a));
            g2d.fillOval((int) x, (int) y, size, size);
        }
    }
}
