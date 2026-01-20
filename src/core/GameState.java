package core;

/**
 * États possibles du jeu
 */
public enum GameState {
    MENU, // Écran d'accueil
    PLAYER_SELECTION, // Sélection du nombre de joueurs
    NETWORK, // Configuration réseau (héberger/rejoindre)
    PLAYING, // En jeu
    PAUSED, // Pause
    GAME_OVER, // Fin de partie
    INFO                    // Écran d'informations
}
