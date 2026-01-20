package ui.components;

import core.GameConfig;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Bouton stylisé néon minimaliste
 */
public class NeonButton {

    private int x, y;
    private int width, height;
    private String text;
    private Color primaryColor;
    private Color hoverColor;
    private boolean hovered;
    private boolean pressed;
    private Runnable onClick;

    // Animation
    private float glowIntensity;
    private float targetGlow;

    public NeonButton(String text, int x, int y, int width, int height) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.primaryColor = GameConfig.NEON_CYAN;
        this.hoverColor = Color.WHITE;
        this.hovered = false;
        this.pressed = false;
        this.glowIntensity = 0.3f;
        this.targetGlow = 0.3f;
    }

    public NeonButton(String text, int x, int y, int width, int height, Color color) {
        this(text, x, y, width, height);
        this.primaryColor = color;
    }

    /**
     * Met à jour l'animation du bouton
     */
    public void update() {
        glowIntensity += (targetGlow - glowIntensity) * 0.15f;
    }

    /**
     * Dessine le bouton - Style minimaliste
     */
    public void render(Graphics2D g2d) {
        Color currentColor = hovered ? hoverColor : primaryColor;

        // Glow extérieur subtil (seulement au survol)
        if (hovered) {
            g2d.setColor(new Color(primaryColor.getRed(), primaryColor.getGreen(),
                    primaryColor.getBlue(), (int) (40 * glowIntensity)));
            g2d.fill(new RoundRectangle2D.Float(x - 4, y - 4,
                    width + 8, height + 8, 14, 14));
        }

        // Fond du bouton
        if (pressed) {
            g2d.setColor(new Color(primaryColor.getRed() / 4, primaryColor.getGreen() / 4,
                    primaryColor.getBlue() / 4, 200));
        } else if (hovered) {
            g2d.setColor(new Color(primaryColor.getRed() / 6, primaryColor.getGreen() / 6,
                    primaryColor.getBlue() / 6, 180));
        } else {
            g2d.setColor(new Color(20, 20, 35, 180));
        }
        g2d.fill(new RoundRectangle2D.Float(x, y, width, height, 10, 10));

        // Bordure
        g2d.setColor(hovered ? primaryColor : new Color(primaryColor.getRed(),
                primaryColor.getGreen(), primaryColor.getBlue(), 120));
        g2d.setStroke(new BasicStroke(hovered ? 2f : 1.5f));
        g2d.draw(new RoundRectangle2D.Float(x, y, width, height, 10, 10));

        // Ligne d'accent en bas (style minimaliste)
        if (hovered) {
            g2d.setColor(primaryColor);
            g2d.fillRect(x + 20, y + height - 3, width - 40, 2);
        }

        // Texte
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        FontMetrics fm = g2d.getFontMetrics();
        int textX = x + (width - fm.stringWidth(text)) / 2;
        int textY = y + (height + fm.getAscent() - fm.getDescent()) / 2;

        // Ombre du texte (très subtile)
        if (hovered) {
            g2d.setColor(new Color(0, 0, 0, 80));
            g2d.drawString(text, textX + 1, textY + 1);
        }

        // Texte principal
        g2d.setColor(hovered ? Color.WHITE : new Color(primaryColor.getRed(),
                primaryColor.getGreen(), primaryColor.getBlue(), 230));
        g2d.drawString(text, textX, textY);
    }

    /**
     * Vérifie si le point est dans le bouton
     */
    public boolean contains(int px, int py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }

    /**
     * Gère le survol
     */
    public void setHovered(boolean hovered) {
        this.hovered = hovered;
        this.targetGlow = hovered ? 1.0f : 0.3f;
    }

    /**
     * Gère le clic
     */
    public void setPressed(boolean pressed) {
        this.pressed = pressed;
    }

    /**
     * Déclenche l'action
     */
    public void click() {
        if (onClick != null) {
            onClick.run();
        }
    }

    public void setOnClick(Runnable onClick) {
        this.onClick = onClick;
    }

    public boolean isHovered() {
        return hovered;
    }

    // Getters
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
