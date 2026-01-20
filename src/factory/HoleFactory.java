package factory;

import factory.entity.Hole;
import java.util.Random;

/**
 * Factory pour créer des trous
 */
public class HoleFactory {

    private static final Random random = new Random();

    // Dimensions des trous
    private static final int MIN_WIDTH = 60;
    private static final int MAX_WIDTH = 120;

    /**
     * Génère un trou de taille aléatoire
     */
    public static Hole generate(double startX) {
        int width = MIN_WIDTH + random.nextInt(MAX_WIDTH - MIN_WIDTH + 1);
        return new Hole(startX, width);
    }

    /**
     * Génère un petit trou
     */
    public static Hole generateSmall(double startX) {
        int width = MIN_WIDTH + random.nextInt(20);
        return new Hole(startX, width);
    }

    /**
     * Génère un grand trou
     */
    public static Hole generateLarge(double startX) {
        int width = MAX_WIDTH - 20 + random.nextInt(40);
        return new Hole(startX, width);
    }
}
