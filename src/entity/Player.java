package entity;

import core.GameConfig;
import graphics.ParticleSystem;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.LinkedList;

/**
 * Classe représentant un joueur
 * Conçue pour supporter le multijoueur local
 */
public class Player {

    // Identification
    private final int playerId;
    private final String playerName;
    private final Color playerColor;

    // Position et mouvement
    private double x;
    private double y;
    private double speedY;
    private double speedX;

    // État
    private Gravity gravity;
    private boolean alive;
    private boolean grounded;
    private boolean falling;
    private boolean switchingGravity;
    private int switchAnimationFrame;

    // Score
    private int score;

    // Visuel
    private LinkedList<Point2D.Double> trail;
    private float alpha;  // Pour effet de fade à la mort
    private ParticleSystem particles;

    // Sprite (si disponible)
    private Image sprite;
    private Image spriteFlipped;

    // Constantes
    private static final double GRAVITY_FORCE = GameConfig.GRAVITY_FORCE * 1.5; // Plus réactif
    private static final double HORIZONTAL_IMPULSE = GameConfig.HORIZONTAL_IMPULSE;
    private static final double MAX_SPEED_Y = GameConfig.MAX_FALL_SPEED * 1.3; // Plus rapide
    private static final double FRICTION = 0.92;
    private static final int SWITCH_ANIMATION_DURATION = 10; // Animation plus courte

    public Player(int playerId, String playerName, Color playerColor) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.playerColor = playerColor;
        this.particles = new ParticleSystem();
        this.trail = new LinkedList<>();
        reset();
    }

    /**
     * Constructeur simplifié pour joueur solo
     */
    public Player() {
        this(1, "Player 1", GameConfig.NEON_CYAN);
    }

    /**
     * Réinitialise le joueur
     */
    public void reset() {
        this.x = GameConfig.PLAYER_START_X;
        this.y = GameConfig.GROUND_Y - GameConfig.PLAYER_HEIGHT;
        this.speedY = 0;
        this.speedX = 0;
        this.gravity = Gravity.DOWN;
        this.alive = true;
        this.grounded = true;
        this.falling = false;
        this.switchingGravity = false;
        this.switchAnimationFrame = 0;
        this.score = 0;
        this.alpha = 1.0f;
        this.trail.clear();
        this.particles.clear();
    }

    /**
     * Met à jour la physique du joueur
     */
    public void update() {
        particles.update();

        if (!alive) {
            // Continuer l'animation de chute même après la mort
            if (falling) {
                speedY += GRAVITY_FORCE * gravity.getDirection();
                speedY = Math.max(-MAX_SPEED_Y, Math.min(MAX_SPEED_Y, speedY));
                y += speedY;
            }
            alpha = Math.max(0, alpha - 0.02f);
            return;
        }

        // Animation de switch
        if (switchingGravity) {
            switchAnimationFrame++;
            if (switchAnimationFrame >= SWITCH_ANIMATION_DURATION) {
                switchingGravity = false;
                switchAnimationFrame = 0;
            }
        }

        // Gravité
        if (!grounded || falling) {
            speedY += GRAVITY_FORCE * gravity.getDirection();
            speedY = Math.max(-MAX_SPEED_Y, Math.min(MAX_SPEED_Y, speedY));
            y += speedY;
        } else {
            speedY = 0;
        }

        // Mouvement horizontal avec friction
        x += speedX;
        speedX *= FRICTION;
        if (Math.abs(speedX) < 0.1) {
            speedX = 0;
        }

        // Limiter la position X
        x = Math.max(0, Math.min(GameConfig.WINDOW_WIDTH - GameConfig.PLAYER_WIDTH, x));

        // Mise à jour de la traînée
        updateTrail();

        // Particules de traînée
        if (grounded && !falling) {
            particles.emitTrail(x + GameConfig.PLAYER_WIDTH / 2,
                              y + GameConfig.PLAYER_HEIGHT, playerColor);
        }

        particles.update();
    }

    /**
     * Met à jour la traînée visuelle
     */
    private void updateTrail() {
        trail.addFirst(new Point2D.Double(x + GameConfig.PLAYER_WIDTH / 2,
                                          y + GameConfig.PLAYER_HEIGHT / 2));

        while (trail.size() > GameConfig.TRAIL_LENGTH) {
            trail.removeLast();
        }
    }

    /**
     * Change la gravité du joueur
     */
    public void switchGravity() {
        if (grounded && !falling && alive) {
            gravity = gravity.opposite();

            // Impulsion initiale forte vers la nouvelle direction
            speedY = GRAVITY_FORCE * gravity.getDirection() * 8;

            speedX = HORIZONTAL_IMPULSE;
            grounded = false;
            switchingGravity = true;
            switchAnimationFrame = 0;

            // Particules lors du switch
            particles.emitGravitySwitch(x + GameConfig.PLAYER_WIDTH / 2,
                                       y + GameConfig.PLAYER_HEIGHT / 2,
                                       playerColor);
        }
    }

    /**
     * Fait mourir le joueur avec effet de particules
     */
    public void die() {
        if (alive) {
            alive = false;
            particles.emitDeath(x + GameConfig.PLAYER_WIDTH / 2,
                              y + GameConfig.PLAYER_HEIGHT / 2, playerColor);
        }
    }

    /**
     * Vérifie si le joueur est complètement hors de l'écran (pour arrêter le rendu)
     */
    public boolean isOffScreen() {
        return y < -GameConfig.PLAYER_HEIGHT * 2 || y > GameConfig.WINDOW_HEIGHT + GameConfig.PLAYER_HEIGHT;
    }

    /**
     * Dessine le joueur et ses effets
     */
    public void render(Graphics2D g2d) {
        if (!alive && alpha <= 0) return;

        // Appliquer l'alpha
        Composite originalComposite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        // Dessiner le joueur
        if (sprite != null) {
            // Utiliser le sprite
            Image currentSprite = (gravity == Gravity.UP) ? spriteFlipped : sprite;
            g2d.drawImage(currentSprite, (int) x, (int) y,
                         GameConfig.PLAYER_WIDTH, GameConfig.PLAYER_HEIGHT, null);
        } else {
            // Dessiner un personnage simplifié
            renderSimplePlayer(g2d);
        }

        g2d.setComposite(originalComposite);

        // Indicateur de gravité
        renderGravityIndicator(g2d);
    }

    /**
     * Dessine le joueur stylisé (sans sprite)
     */
    private void renderStylizedPlayer(Graphics2D g2d) {
        int px = (int) x;
        int py = (int) y;
        int w = GameConfig.PLAYER_WIDTH;
        int h = GameConfig.PLAYER_HEIGHT;

        // Corps principal - forme arrondie
        GradientPaint bodyGradient = new GradientPaint(
            px, py, playerColor,
            px + w, py + h, playerColor.darker().darker()
        );
        g2d.setPaint(bodyGradient);
        g2d.fillRoundRect(px + 5, py + 10, w - 10, h - 15, 10, 10);

        // Tête
        g2d.setColor(playerColor);
        g2d.fillOval(px + 8, py, w - 16, 20);

        // Visière/yeux (style cyberpunk)
        g2d.setColor(GameConfig.NEON_PINK);
        g2d.fillRect(px + 10, py + 6, w - 20, 6);

        // Jambes (adaptées selon gravité)
        g2d.setColor(playerColor.darker());
        if (gravity == Gravity.DOWN) {
            g2d.fillRect(px + 10, py + h - 10, 8, 10);
            g2d.fillRect(px + w - 18, py + h - 10, 8, 10);
        } else {
            g2d.fillRect(px + 10, py, 8, 10);
            g2d.fillRect(px + w - 18, py, 8, 10);
        }

        // Contour lumineux
        g2d.setColor(new Color(playerColor.getRed(), playerColor.getGreen(),
                               playerColor.getBlue(), 150));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(px + 5, py + 10, w - 10, h - 15, 10, 10);
    }

    /**
     * Dessine le joueur simplifié (pour performance)
     */
    private void renderSimplePlayer(Graphics2D g2d) {
        int px = (int) x;
        int py = (int) y;
        int w = GameConfig.PLAYER_WIDTH;
        int h = GameConfig.PLAYER_HEIGHT;

        // Effet de squash & stretch léger lors du changement de gravité
        float scaleX = 1.0f;
        float scaleY = 1.0f;

        if (switchingGravity) {
            // Étirement vertical léger au début
            float progress = (float) switchAnimationFrame / SWITCH_ANIMATION_DURATION;
            if (progress < 0.3f) {
                // Phase d'étirement léger
                scaleX = 1.0f - 0.1f * (progress / 0.3f);  // Réduit de 0.25 à 0.1
                scaleY = 1.0f + 0.12f * (progress / 0.3f); // Réduit de 0.3 à 0.12
            } else {
                // Phase de retour à la normale
                float returnProgress = (progress - 0.3f) / 0.7f;
                scaleX = 0.9f + 0.1f * returnProgress;
                scaleY = 1.12f - 0.12f * returnProgress;
            }
        } else if (!grounded && Math.abs(speedY) > 8) {
            // Léger étirement pendant la chute/montée très rapide seulement
            scaleX = 0.95f;  // Réduit de 0.9
            scaleY = 1.06f;  // Réduit de 1.15
        } else if (grounded) {
            // Très léger écrasement au sol
            scaleX = 1.02f;  // Réduit de 1.05
            scaleY = 0.98f;  // Réduit de 0.95
        }

        // Appliquer la transformation
        int scaledW = (int) (w * scaleX);
        int scaledH = (int) (h * scaleY);
        int offsetX = (w - scaledW) / 2;
        int offsetY = (h - scaledH) / 2;

        // Corps simple avec échelle
        g2d.setColor(playerColor);
        g2d.fillRoundRect(px + offsetX + 5, py + offsetY + 5, scaledW - 10, scaledH - 10, 8, 8);

        // Visière (position ajustée)
        g2d.setColor(GameConfig.NEON_PINK);
        int visY = py + offsetY + (int)(12 * scaleY);
        g2d.fillRect(px + offsetX + 10, visY, scaledW - 20, (int)(5 * scaleY));

        // Contour lumineux
        g2d.setColor(playerColor.brighter());
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(px + offsetX + 5, py + offsetY + 5, scaledW - 10, scaledH - 10, 8, 8);

        // Effet de flash lors du switch
        if (switchingGravity && switchAnimationFrame < 5) {
            g2d.setColor(new Color(255, 255, 255, 150 - switchAnimationFrame * 30));
            g2d.fillRoundRect(px + offsetX + 5, py + offsetY + 5, scaledW - 10, scaledH - 10, 8, 8);
        }
    }

    /**
     * Dessine la traînée
     */
    private void renderTrail(Graphics2D g2d) {
        if (trail.size() < 2) return;

        for (int i = 0; i < trail.size() - 1; i++) {
            Point2D.Double p = trail.get(i);
            float trailAlpha = (float) (trail.size() - i) / trail.size() * 0.5f * alpha;
            g2d.setColor(new Color(playerColor.getRed(), playerColor.getGreen(),
                                   playerColor.getBlue(), (int) (trailAlpha * 255)));
            int size = (int) ((trail.size() - i) / 2);
            g2d.fillOval((int) p.x - size/2, (int) p.y - size/2, size, size);
        }
    }

    /**
     * Dessine l'indicateur de gravité
     */
    private void renderGravityIndicator(Graphics2D g2d) {
        if (!alive) return;

        Color indicatorColor = grounded && !falling ? Color.GREEN : Color.GRAY;
        g2d.setColor(indicatorColor);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));

        String arrow = gravity == Gravity.DOWN ? "▼" : "▲";
        int arrowY = gravity == Gravity.DOWN ?
                    (int) y + GameConfig.PLAYER_HEIGHT + 15 : (int) y - 5;
        g2d.drawString(arrow, (int) x + GameConfig.PLAYER_WIDTH/2 - 5, arrowY);
    }

    // === GETTERS & SETTERS ===

    public int getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public Color getPlayerColor() { return playerColor; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public int getWidth() { return GameConfig.PLAYER_WIDTH; }
    public int getHeight() { return GameConfig.PLAYER_HEIGHT; }

    public Gravity getGravity() { return gravity; }

    public boolean isAlive() { return alive; }
    public boolean isGrounded() { return grounded; }
    public void setGrounded(boolean grounded) {
        this.grounded = grounded;
        if (grounded) {
            speedY = 0;
            speedX *= 0.5;
        }
    }

    public boolean isFalling() { return falling; }
    public void setFalling(boolean falling) { this.falling = falling; }

    public int getScore() { return score; }
    public void addScore(int points) { this.score += points; }
    public void setScore(int score) { this.score = score; }

    // Méthodes pour le réseau
    public double getVelocityY() { return speedY; }
    public void setVelocityY(double vy) { this.speedY = vy; }

    public void setGravity(Gravity gravity) { this.gravity = gravity; }

    public void setSprite(Image sprite, Image spriteFlipped) {
        this.sprite = sprite;
        this.spriteFlipped = spriteFlipped;
    }

    /**
     * Rectangle de collision
     */
    public Rectangle getBounds() {
        return new Rectangle((int) x + 5, (int) y + 5,
                            GameConfig.PLAYER_WIDTH - 10, GameConfig.PLAYER_HEIGHT - 10);
    }
}
