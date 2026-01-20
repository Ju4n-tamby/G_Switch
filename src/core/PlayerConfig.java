package core;

import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Configuration pour un joueur (id, couleur, touches)
 */
public class PlayerConfig {
    
    private final int playerId;
    private final String playerName;
    private final Color playerColor;
    private final int keyGravitySwitch;  // Touche pour inverser la gravité
    private final int keyJump;            // Touche pour sauter (alternative)
    
    // Couleurs prédéfinies pour les joueurs
    public static final Color[] PLAYER_COLORS = {
        GameConfig.NEON_CYAN,      // Joueur 1
        GameConfig.NEON_PURPLE,    // Joueur 2
        GameConfig.NEON_PINK,      // Joueur 3
        GameConfig.NEON_BLUE,      // Joueur 4
    };
    
    // Schémas de touches prédéfinis
    public static final int[][] KEY_SCHEMES = {
        { KeyEvent.VK_SPACE, KeyEvent.VK_UP },      // Joueur 1: ESPACE / HAUT
        { KeyEvent.VK_W, KeyEvent.VK_Z },           // Joueur 2: W / Z
        { KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT }, // Joueur 3: CTRL / SHIFT
        { KeyEvent.VK_ALT, KeyEvent.VK_ALT_GRAPH }  // Joueur 4: ALT / ALT GR
    };
    
    public static final String[] KEY_NAMES = {
        "ESPACE / HAUT",
        "W / Z",
        "CTRL / SHIFT",
        "ALT / ALT GR"
    };
    
    public PlayerConfig(int playerId, String playerName, Color playerColor, 
                       int keyGravitySwitch, int keyJump) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.playerColor = playerColor;
        this.keyGravitySwitch = keyGravitySwitch;
        this.keyJump = keyJump;
    }
    
    /**
     * Crée une configuration avec schéma prédéfini
     */
    public static PlayerConfig createWithScheme(int playerId) {
        if (playerId < 1 || playerId > 4) {
            throw new IllegalArgumentException("Player ID doit être entre 1 et 4");
        }
        
        int schemeIndex = playerId - 1;
        return new PlayerConfig(
            playerId,
            "Joueur " + playerId,
            PLAYER_COLORS[schemeIndex],
            KEY_SCHEMES[schemeIndex][0],
            KEY_SCHEMES[schemeIndex][1]
        );
    }
    
    // Getters
    public int getPlayerId() {
        return playerId;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public Color getPlayerColor() {
        return playerColor;
    }
    
    public int getKeyGravitySwitch() {
        return keyGravitySwitch;
    }
    
    public int getKeyJump() {
        return keyJump;
    }
    
    public static String getKeyName(int keyCode) {
        return KeyEvent.getKeyText(keyCode);
    }
    
    @Override
    public String toString() {
        return playerName + " - Touches: " + getKeyName(keyGravitySwitch) + " / " + getKeyName(keyJump);
    }
}
