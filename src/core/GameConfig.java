package core;

import java.awt.Color;

/**
 * Configuration globale du jeu - Constantes et paramètres
 */
public final class GameConfig {

    // === FENÊTRE ===
    public static final int WINDOW_WIDTH = 1600;
    public static final int WINDOW_HEIGHT = 900;
    public static final String GAME_TITLE = "VOID RUNNER";
    public static final String GAME_SUBTITLE = "Navigate the Void";
    public static final String GAME_VERSION = "v2.0";
    public static final int TARGET_FPS = 60;
    public static final int FRAME_TIME = 1000 / TARGET_FPS; // ~16ms

    // === GAMEPLAY ===
    public static final int GROUND_Y = 580;           // Position du sol
    public static final int CEILING_Y = 140;          // Position du plafond
    public static final int PLATFORM_HEIGHT = 40;     // Épaisseur des plateformes
    public static final double GAME_SPEED = 5.0;      // Vitesse de défilement
    public static final double GRAVITY_FORCE = 0.8;
    public static final double MAX_FALL_SPEED = 15.0;

    // === SPAWN ===
    public static final int HOLE_SPAWN_INTERVAL = 180;     // Frames entre trous
    public static final int OBSTACLE_SPAWN_INTERVAL = 150; // Frames entre obstacles
    public static final int MIN_SPAWN_DISTANCE = 300;      // Distance min entre éléments

    // === JOUEUR ===
    public static final int PLAYER_WIDTH = 40;
    public static final int PLAYER_HEIGHT = 50;
    public static final int PLAYER_START_X = 150;
    public static final double HORIZONTAL_IMPULSE = 3.0;
    public static final int TRAIL_LENGTH = 8;        // Longueur de la traînée (réduit pour perf)

    // === COULEURS NÉON MINIMALISTE ===
    public static final Color NEON_CYAN = new Color(0, 255, 255);
    public static final Color NEON_PINK = new Color(255, 0, 128);
    public static final Color NEON_PURPLE = new Color(138, 43, 226);
    public static final Color NEON_BLUE = new Color(65, 105, 225);
    public static final Color NEON_YELLOW = new Color(255, 215, 0);
    public static final Color NEON_GREEN = new Color(0, 255, 127);
    public static final Color NEON_ORANGE = new Color(255, 140, 0);
    public static final Color DARK_BG = new Color(8, 8, 16);
    public static final Color DARK_SURFACE = new Color(15, 15, 25);
    public static final Color DARK_CARD = new Color(20, 20, 35);
    public static final Color DARK_PURPLE = new Color(15, 10, 25);
    public static final Color GRID_COLOR = new Color(255, 255, 255, 15);
    public static final Color TEXT_PRIMARY = new Color(240, 240, 250);
    public static final Color TEXT_SECONDARY = new Color(150, 150, 170);
    public static final Color PLATFORM_COLOR = new Color(0, 255, 255, 200);
    public static final Color PLATFORM_GLOW = new Color(0, 255, 255, 100);

    // === PARTICULES ===
    public static final int DEATH_PARTICLE_COUNT = 25;  // Réduit pour performance
    public static final int TRAIL_PARTICLE_COUNT = 2;
    public static final double PARTICLE_LIFE = 40;   // Frames

    // === CHEMINS RESSOURCES ===
    public static final String RESOURCES_PATH = "resources/";
    public static final String IMAGES_PATH = RESOURCES_PATH + "images/";
    public static final String SPRITES_PATH = RESOURCES_PATH + "sprites/";

    // === MODE DE JEU ===
    public enum GameMode {
        SOLO,           // Joueur seul
        LOCAL,          // Multijoueur local (même écran)
        NETWORK         // Multijoueur en réseau
    }

    private GameConfig() {
        // Empêcher l'instanciation
    }
}
