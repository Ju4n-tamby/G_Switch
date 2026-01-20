package factory;

import core.GameConfig;
import factory.entity.Obstacle;
import factory.entity.Obstacle.ObstacleType;

import java.util.Random;

/**
 * Factory pour créer des obstacles variés
 */
public class ObstacleFactory {

    private static final Random random = new Random();

    /**
     * Génère un obstacle aléatoire
     */
    public static Obstacle generate(double startX) {
        // Type d'obstacle aléatoire
        boolean onGround = random.nextBoolean();
        boolean isSpike = random.nextFloat() < 0.3f; // 30% de chance d'être une pointe

        // Dimensions
        int width, height;
        ObstacleType type;

        if (isSpike) {
            width = 30 + random.nextInt(30);  // 30-60
            height = 50 + random.nextInt(40); // 50-90
            type = ObstacleType.SPIKE;
        } else {
            width = 30 + random.nextInt(50);   // 30-80
            height = 50 + random.nextInt(70);  // 50-120
            type = onGround ? ObstacleType.GROUND : ObstacleType.CEILING;
        }

        // Position Y
        double y;
        if (onGround) {
            y = GameConfig.GROUND_Y - height;
        } else {
            y = GameConfig.CEILING_Y;
            type = isSpike ? ObstacleType.SPIKE : ObstacleType.CEILING;
        }

        return new Obstacle(startX, y, width, height, type);
    }

    /**
     * Génère un obstacle sur le sol
     */
    public static Obstacle generateGround(double startX) {
        int width = 30 + random.nextInt(50);
        int height = 50 + random.nextInt(70);
        double y = GameConfig.GROUND_Y - height;
        return new Obstacle(startX, y, width, height, ObstacleType.GROUND);
    }

    /**
     * Génère un obstacle au plafond
     */
    public static Obstacle generateCeiling(double startX) {
        int width = 30 + random.nextInt(50);
        int height = 50 + random.nextInt(70);
        return new Obstacle(startX, GameConfig.CEILING_Y, width, height, ObstacleType.CEILING);
    }

    /**
     * Génère une pointe
     */
    public static Obstacle generateSpike(double startX, boolean onGround) {
        int width = 30 + random.nextInt(30);
        int height = 50 + random.nextInt(40);
        double y = onGround ? GameConfig.GROUND_Y - height : GameConfig.CEILING_Y;
        return new Obstacle(startX, y, width, height, ObstacleType.SPIKE);
    }
}
