package input;

import core.GameEngine;
import core.GameState;
import core.PlayerConfig;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Gestionnaire centralisé des entrées clavier (support multijoueur)
 */
public class InputHandler implements KeyListener {

    private GameEngine engine;
    private Set<Integer> pressedKeys;
    private InputCallback callback;
    private List<PlayerConfig> playerConfigs;

    public interface InputCallback {

        void onPlayerGravitySwitch(int playerId);

        void onPausePressed();

        void onRestartPressed();
    }

    public InputHandler(GameEngine engine) {
        this.engine = engine;
        this.pressedKeys = new HashSet<>();
        this.playerConfigs = new ArrayList<>();
    }

    public void setCallback(InputCallback callback) {
        this.callback = callback;
    }

    public void setPlayerConfigs(List<PlayerConfig> configs) {
        this.playerConfigs = new ArrayList<>(configs);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Non utilisé
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        GameState state = engine.getCurrentState();

        if (state == GameState.PLAYING) {
            // Vérifier si c'est une touche de contrôle de joueur
            for (PlayerConfig config : playerConfigs) {
                if (keyCode == config.getKeyGravitySwitch() || keyCode == config.getKeyJump()) {
                    if (!pressedKeys.contains(keyCode)) {
                        pressedKeys.add(keyCode);
                        if (callback != null) {
                            callback.onPlayerGravitySwitch(config.getPlayerId());
                        }
                    }
                    return;
                }
            }
            
            // Si pas de config (mode réseau ou solo par défaut), ESPACE active le joueur 0
            if (playerConfigs.isEmpty() && keyCode == KeyEvent.VK_SPACE) {
                if (!pressedKeys.contains(keyCode)) {
                    pressedKeys.add(keyCode);
                    if (callback != null) {
                        callback.onPlayerGravitySwitch(0);
                    }
                }
                return;
            }
        }

        // Éviter les répétitions pour les autres touches
        if (pressedKeys.contains(keyCode)) {
            return;
        }
        pressedKeys.add(keyCode);

        switch (keyCode) {
            case KeyEvent.VK_ESCAPE:
                handleEscape(state);
                break;

            case KeyEvent.VK_R:
                if ((state == GameState.GAME_OVER || state == GameState.PLAYING) && callback != null) {
                    callback.onRestartPressed();
                }
                break;

            case KeyEvent.VK_P:
                if (state == GameState.PLAYING || state == GameState.PAUSED) {
                    if (callback != null) {
                        callback.onPausePressed();
                    }
                }
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        pressedKeys.remove(e.getKeyCode());
    }

    private void handleEscape(GameState state) {
        switch (state) {
            case PLAYING:
                engine.setState(GameState.PAUSED);
                break;
            case PAUSED:
                engine.resumeGame();
                break;
            case INFO:
            case NETWORK:
            case PLAYER_SELECTION:
                engine.setState(GameState.MENU);
                break;
            case GAME_OVER:
            case MENU:
                // Rien à faire
                break;
        }
    }

    public boolean isKeyPressed(int keyCode) {
        return pressedKeys.contains(keyCode);
    }

    public void clearKeys() {
        pressedKeys.clear();
    }
}
