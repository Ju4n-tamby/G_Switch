package core;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import ui.GameWindow;
import ui.panels.*;

/**
 * Moteur principal du jeu - Gère les états et les transitions
 */
public class GameEngine {

    private GameWindow window;
    private GameState currentState;
    private GameConfig.GameMode gameMode;
    private CardLayout cardLayout;
    private JPanel mainContainer;

    // Panels
    private MenuPanel menuPanel;
    private PlayerSelectionPanel playerSelectionPanel;
    private NetworkPanel networkPanel;
    private GamePanel gamePanel;
    private InfoPanel infoPanel;

    // Configurations des joueurs
    private List<PlayerConfig> playerConfigs;

    // Réseau (simulé pour l'instant)
    private int currentPing = 0;

    public GameEngine() {
        this.currentState = GameState.MENU;
        this.gameMode = GameConfig.GameMode.SOLO;
        this.playerConfigs = new ArrayList<>();
        initializeUI();
    }

    private void initializeUI() {
        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);
        mainContainer.setBackground(GameConfig.DARK_BG);

        // Création des panels
        menuPanel = new MenuPanel(this);
        playerSelectionPanel = new PlayerSelectionPanel(this);
        networkPanel = new NetworkPanel(this);
        gamePanel = new GamePanel(this);
        infoPanel = new InfoPanel(this);

        // Ajout au conteneur
        mainContainer.add(menuPanel, "MENU");
        mainContainer.add(playerSelectionPanel, "PLAYER_SELECTION");
        mainContainer.add(networkPanel, "NETWORK");
        mainContainer.add(gamePanel, "GAME");
        mainContainer.add(infoPanel, "INFO");

        // Création de la fenêtre
        window = new GameWindow(mainContainer);
    }

    public void start() {
        setState(GameState.MENU);
        window.setVisible(true);
    }

    public void setState(GameState newState) {
        this.currentState = newState;

        switch (newState) {
            case MENU:
                cardLayout.show(mainContainer, "MENU");
                menuPanel.requestFocusInWindow();
                break;
            case PLAYER_SELECTION:
                cardLayout.show(mainContainer, "PLAYER_SELECTION");
                playerSelectionPanel.requestFocusInWindow();
                break;
            case NETWORK:
                cardLayout.show(mainContainer, "NETWORK");
                networkPanel.onPanelShown();
                networkPanel.requestFocusInWindow();
                break;
            case PLAYING:
                cardLayout.show(mainContainer, "GAME");
                gamePanel.startGame();
                gamePanel.requestFocusInWindow();
                break;
            case INFO:
                cardLayout.show(mainContainer, "INFO");
                infoPanel.requestFocusInWindow();
                break;
            case PAUSED:
                gamePanel.pauseGame();
                break;
            case GAME_OVER:
                gamePanel.gameOver();
                break;
        }
    }

    public GameState getCurrentState() {
        return currentState;
    }

    public void resumeGame() {
        if (currentState == GameState.PAUSED) {
            currentState = GameState.PLAYING;
            gamePanel.resumeGame();
        }
    }

    public void restartGame() {
        gamePanel.resetGame();
        setState(GameState.PLAYING);
    }

    public void exitGame() {
        System.out.println("👋 Merci d'avoir joué à VOID RUNNER !");
        System.exit(0);
    }

    public GameWindow getWindow() {
        return window;
    }

    public void setPlayerConfigs(List<PlayerConfig> configs) {
        this.playerConfigs = new ArrayList<>(configs);
        gamePanel.setPlayerConfigs(configs);
    }

    public List<PlayerConfig> getPlayerConfigs() {
        return playerConfigs;
    }

    // === Gestion du mode de jeu ===

    public void setGameMode(GameConfig.GameMode mode) {
        this.gameMode = mode;
        gamePanel.setGameMode(mode);
    }

    public GameConfig.GameMode getGameMode() {
        return gameMode;
    }

    public boolean isNetworkMode() {
        return gameMode == GameConfig.GameMode.NETWORK;
    }

    // === Gestion du ping (pour le mode réseau) ===

    public void setPing(int ping) {
        this.currentPing = ping;
    }

    public int getPing() {
        return currentPing;
    }
}
