package ui.panels;

import core.GameConfig;
import core.GameEngine;
import core.GameState;
import graphics.ResourceManager;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import ui.components.NeonButton;

/**
 * Panel d'informations sur le jeu - Style néon minimaliste
 */
public class InfoPanel extends JPanel {

    private GameEngine engine;
    private Timer animationTimer;
    private NeonButton backButton;
    private ResourceManager resources;
    private float glowPhase;

    // Développeurs
    private static final String[] DEVELOPERS = {"Juan", "Harry", "Aro", "Sedra", "Mahery"};
    private static final Color[] DEV_COLORS = {
        GameConfig.NEON_CYAN,
        GameConfig.NEON_PURPLE,
        GameConfig.NEON_PINK,
        GameConfig.NEON_GREEN,
        GameConfig.NEON_ORANGE
    };

    public InfoPanel(GameEngine engine) {
        this.engine = engine;
        this.resources = ResourceManager.getInstance();

        setPreferredSize(new Dimension(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT));
        setBackground(GameConfig.DARK_BG);
        setFocusable(true);

        initButton();
        setupMouseListeners();
        setupKeyBindings();

        animationTimer = new Timer(16, e -> {
            updateAnimations();
            repaint();
        });
        animationTimer.start();
    }

    private void initButton() {
        int buttonWidth = 180;
        int buttonHeight = 50;
        backButton = new NeonButton("← RETOUR",
                GameConfig.WINDOW_WIDTH / 2 - buttonWidth / 2,
                GameConfig.WINDOW_HEIGHT - 100,
                buttonWidth, buttonHeight, GameConfig.NEON_PINK);
        backButton.setOnClick(() -> engine.setState(GameState.MENU));
    }

    private void setupMouseListeners() {
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                backButton.setHovered(backButton.contains(e.getX(), e.getY()));
                repaint();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (backButton.contains(e.getX(), e.getY())) {
                    backButton.setPressed(true);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (backButton.isHovered()) {
                    backButton.click();
                }
                backButton.setPressed(false);
            }
        });
    }

    private void setupKeyBindings() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    engine.setState(GameState.MENU);
                }
            }
        });
    }

    private void updateAnimations() {
        glowPhase += 0.03f;
        backButton.update();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        renderBackground(g2d);
        renderTitle(g2d);
        renderContent(g2d);
        renderDevelopers(g2d);
        backButton.render(g2d);
    }

    private void renderBackground(Graphics2D g2d) {
        // Utiliser l'image de fond si disponible
        if (resources.hasMenuBackground()) {
            resources.drawMenuBackground(g2d, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        } else {
            g2d.setColor(GameConfig.DARK_BG);
            g2d.fillRect(0, 0, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        }

        // Grille subtile
        g2d.setColor(GameConfig.GRID_COLOR);
        g2d.setStroke(new BasicStroke(1));
        for (int x = 0; x < GameConfig.WINDOW_WIDTH; x += 60) {
            g2d.drawLine(x, 0, x, GameConfig.WINDOW_HEIGHT);
        }
        for (int y = 0; y < GameConfig.WINDOW_HEIGHT; y += 60) {
            g2d.drawLine(0, y, GameConfig.WINDOW_WIDTH, y);
        }

        // Lueur subtile
        RadialGradientPaint radial = new RadialGradientPaint(
                GameConfig.WINDOW_WIDTH / 2f, 200,
                400,
                new float[]{0f, 1f},
                new Color[]{new Color(138, 43, 226, 15), new Color(0, 0, 0, 0)}
        );
        g2d.setPaint(radial);
        g2d.fillRect(0, 0, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
    }

    private void renderTitle(Graphics2D g2d) {
        String title = "À PROPOS";
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        FontMetrics fm = g2d.getFontMetrics();
        int x = (GameConfig.WINDOW_WIDTH - fm.stringWidth(title)) / 2;
        int y = 80;

        // Glow
        float glow = (float) (Math.sin(glowPhase) * 0.3 + 0.7);
        g2d.setColor(new Color(0, 255, 255, (int) (50 * glow)));
        g2d.drawString(title, x, y);

        g2d.setColor(GameConfig.TEXT_PRIMARY);
        g2d.drawString(title, x, y);

        // Ligne sous le titre
        int lineY = y + 25;
        g2d.setStroke(new BasicStroke(1.5f));
        GradientPaint lineGrad = new GradientPaint(
                x, lineY, new Color(0, 255, 255, 0),
                x + fm.stringWidth(title) / 2, lineY, GameConfig.NEON_CYAN
        );
        g2d.setPaint(lineGrad);
        g2d.drawLine(x, lineY, x + fm.stringWidth(title) / 2, lineY);

        lineGrad = new GradientPaint(
                x + fm.stringWidth(title) / 2, lineY, GameConfig.NEON_CYAN,
                x + fm.stringWidth(title), lineY, new Color(0, 255, 255, 0)
        );
        g2d.setPaint(lineGrad);
        g2d.drawLine(x + fm.stringWidth(title) / 2, lineY, x + fm.stringWidth(title), lineY);
    }

    private void renderContent(Graphics2D g2d) {
        int leftX = 150;
        int rightX = GameConfig.WINDOW_WIDTH / 2 + 50;
        int startY = 150;

        // Section Comment Jouer
        renderCard(g2d, "🎮 COMMENT JOUER", leftX, startY, 500, 180, GameConfig.NEON_CYAN, new String[]{
            "▸ ESPACE ou CLIC GAUCHE pour inverser la gravité",
            "▸ Évitez les obstacles rouges",
            "▸ Ne tombez pas dans les trous !",
            "▸ Score de 5 = Ligne d'arrivée !"
        });

        // Section Contrôles
        renderCard(g2d, "⌨️ CONTRÔLES", rightX, startY, 500, 180, GameConfig.NEON_PURPLE, new String[]{
            "ESPACE / CLIC  →  Inverser la gravité",
            "ECHAP          →  Pause / Menu",
            "R              →  Rejouer (Game Over)",
            "Le vainqueur est affiché à la victoire"
        });

        // Section Modes de jeu
        renderCard(g2d, "🕹️ MODES DE JEU", leftX, startY + 210, 500, 150, GameConfig.NEON_PINK, new String[]{
            "▸ Solo : Battez votre meilleur score !",
            "▸ Local : 2-5 joueurs sur le même clavier",
            "▸ Réseau : Multijoueur LAN automatique"
        });

        // Section Mode Réseau
        renderCard(g2d, "🌐 MODE RÉSEAU", rightX, startY + 210, 500, 150, GameConfig.NEON_GREEN, new String[]{
            "▸ Héberger : Créez un lobby, lancez quand prêt",
            "▸ Rejoindre : Détection auto des serveurs",
            "▸ Hôte : RETOUR LOBBY pour revenir au lobby",
            "▸ Personne ne peut rejoindre en cours de partie"
        });
    }

    private void renderCard(Graphics2D g2d, String title, int x, int y, int width, int height,
            Color accentColor, String[] lines) {
        // Fond de la carte
        g2d.setColor(GameConfig.DARK_CARD);
        g2d.fill(new RoundRectangle2D.Float(x, y, width, height, 12, 12));

        // Bordure colorée
        g2d.setColor(new Color(accentColor.getRed(), accentColor.getGreen(),
                accentColor.getBlue(), 100));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.draw(new RoundRectangle2D.Float(x, y, width, height, 12, 12));

        // Ligne d'accent en haut
        g2d.setColor(accentColor);
        g2d.fillRect(x + 20, y, 60, 3);

        // Titre de la carte
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.setColor(accentColor);
        g2d.drawString(title, x + 20, y + 35);

        // Contenu
        g2d.setFont(new Font("Arial", Font.PLAIN, 15));
        g2d.setColor(GameConfig.TEXT_SECONDARY);

        int lineY = y + 65;
        for (String line : lines) {
            g2d.drawString(line, x + 25, lineY);
            lineY += 26;
        }
    }

    private void renderDevelopers(Graphics2D g2d) {
        int centerX = GameConfig.WINDOW_WIDTH / 2;
        int y = 570;

        // Titre
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.setColor(GameConfig.TEXT_PRIMARY);
        String devTitle = "👨‍💻 DÉVELOPPEURS";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(devTitle, centerX - fm.stringWidth(devTitle) / 2, y);

        // Noms des développeurs en ligne
        int spacing = 150;
        int startX = centerX - (DEVELOPERS.length - 1) * spacing / 2;
        int devY = y + 50;

        g2d.setFont(new Font("Arial", Font.BOLD, 16));

        for (int i = 0; i < DEVELOPERS.length; i++) {
            int devX = startX + i * spacing;
            Color color = DEV_COLORS[i % DEV_COLORS.length];

            // Cercle coloré
            float pulse = (float) (Math.sin(glowPhase + i * 0.5) * 0.3 + 0.7);
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(),
                    (int) (50 * pulse)));
            g2d.fillOval(devX - 25, devY - 25, 50, 50);

            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(devX - 20, devY - 20, 40, 40);

            // Initiale
            String initial = DEVELOPERS[i].substring(0, 1);
            fm = g2d.getFontMetrics();
            g2d.setColor(GameConfig.TEXT_PRIMARY);
            g2d.drawString(initial, devX - fm.stringWidth(initial) / 2, devY + 6);

            // Nom sous le cercle
            g2d.setFont(new Font("Arial", Font.PLAIN, 14));
            fm = g2d.getFontMetrics();
            g2d.setColor(color);
            g2d.drawString(DEVELOPERS[i], devX - fm.stringWidth(DEVELOPERS[i]) / 2, devY + 45);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
        }
    }
}
