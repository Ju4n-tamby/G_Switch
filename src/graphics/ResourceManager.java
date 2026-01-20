package graphics;

import core.GameConfig;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Gestionnaire de ressources graphiques Charge et met en cache les images pour
 * une meilleure performance
 */
public class ResourceManager {

    private static ResourceManager instance;

    // Images de fond
    private BufferedImage menuBackground;
    private BufferedImage gameBackground;
    private BufferedImage neonGrid;

    // Version assombrie/traitée des images
    private BufferedImage menuBackgroundDark;

    private ResourceManager() {
        loadResources();
    }

    public static ResourceManager getInstance() {
        if (instance == null) {
            instance = new ResourceManager();
        }
        return instance;
    }

    private void loadResources() {
        try {
            // Charger les images de fond
            File menuBgFile = new File(GameConfig.IMAGES_PATH + "menu_bg.jpg");
            if (menuBgFile.exists()) {
                menuBackground = ImageIO.read(menuBgFile);
                menuBackgroundDark = createDarkOverlay(menuBackground, 0.7f);
                System.out.println("✓ Image menu_bg.jpg chargée");
            }

            File neonGridFile = new File(GameConfig.IMAGES_PATH + "neon_grid.jpg");
            if (neonGridFile.exists()) {
                neonGrid = ImageIO.read(neonGridFile);
                System.out.println("✓ Image neon_grid.jpg chargée");
            }

        } catch (IOException e) {
            System.out.println("⚠ Impossible de charger certaines ressources: " + e.getMessage());
        }
    }

    /**
     * Crée une version assombrie de l'image avec une teinte
     */
    private BufferedImage createDarkOverlay(BufferedImage original, float darkness) {
        int width = original.getWidth();
        int height = original.getHeight();

        BufferedImage dark = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = dark.createGraphics();

        // Dessiner l'image originale
        g2d.drawImage(original, 0, 0, null);

        // Appliquer un overlay sombre avec teinte violette
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, darkness));
        g2d.setColor(new Color(8, 8, 20));
        g2d.fillRect(0, 0, width, height);

        // Ajouter une légère teinte cyan/violet
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
        GradientPaint gradient = new GradientPaint(
                0, 0, new Color(0, 255, 255, 50),
                width, height, new Color(138, 43, 226, 50)
        );
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, width, height);

        g2d.dispose();
        return dark;
    }

    /**
     * Dessine le fond du menu avec l'image si disponible
     */
    public void drawMenuBackground(Graphics2D g2d, int width, int height) {
        if (menuBackgroundDark != null) {
            // Dessiner l'image redimensionnée pour couvrir tout l'écran
            g2d.drawImage(menuBackgroundDark, 0, 0, width, height, null);
        } else {
            // Fallback: fond dégradé
            g2d.setColor(GameConfig.DARK_BG);
            g2d.fillRect(0, 0, width, height);
        }
    }

    /**
     * Dessine la grille néon si disponible
     */
    public void drawNeonGrid(Graphics2D g2d, int width, int height, float alpha) {
        if (neonGrid != null) {
            Composite oldComposite = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2d.drawImage(neonGrid, 0, 0, width, height, null);
            g2d.setComposite(oldComposite);
        }
    }

    // Getters
    public BufferedImage getMenuBackground() {
        return menuBackground;
    }

    public BufferedImage getMenuBackgroundDark() {
        return menuBackgroundDark;
    }

    public BufferedImage getNeonGrid() {
        return neonGrid;
    }

    public boolean hasMenuBackground() {
        return menuBackgroundDark != null;
    }

    public boolean hasNeonGrid() {
        return neonGrid != null;
    }
}
