package network;

import core.GameConfig;
import core.GameEngine;
import entity.Hole;
import entity.Obstacle;
import entity.Player;
import java.util.*;

/**
 * Gestionnaire réseau central
 * Fait le pont entre le jeu et les composants réseau (serveur/client)
 * Simplifie l'utilisation du réseau depuis l'UI
 */
public class NetworkManager {

    private static NetworkManager instance;

    private GameServer server;
    private GameClient client;
    private GameEngine engine;

    // État
    private NetworkMode mode = NetworkMode.NONE;
    private boolean inLobby;
    private boolean inGame;

    // Callbacks
    private NetworkListener listener;

    public enum NetworkMode {
        NONE,       // Pas de réseau
        HOST,       // Héberge une partie
        CLIENT      // Connecté à un serveur
    }

    /**
     * Interface de callback pour les événements réseau
     */
    public interface NetworkListener {
        void onModeChanged(NetworkMode mode);
        void onPlayerListUpdate(List<PlayerInfo> players);
        void onChatMessage(String playerName, String message, boolean isSystem);
        void onGameStart();
        void onReturnToLobby();
        void onDisconnected(String reason);
        void onError(String error);
        void onPingUpdate(int ping);
        void onServerFound(LANDiscovery.ServerInfo server);
        void onServerLost(String address);
    }

    /**
     * Informations sur un joueur dans le lobby
     */
    public static class PlayerInfo {
        public int id;
        public String name;
        public String colorHex;
        public boolean ready;
        public boolean isHost;

        @Override
        public String toString() {
            return name + (isHost ? " (Hôte)" : "") + (ready ? " ✓" : "");
        }
    }

    private NetworkManager() {}

    public static NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }

    public void setEngine(GameEngine engine) {
        this.engine = engine;
    }

    // ==================== HÉBERGER UNE PARTIE ====================

    /**
     * Démarre un serveur et devient l'hôte
     */
    public boolean hostGame(String playerName) {
        if (mode != NetworkMode.NONE) {
            stopNetwork();
        }

        String serverName = "Partie de " + playerName;
        server = new GameServer(serverName);

        server.setListener(new GameServer.ServerListener() {
            @Override
            public void onPlayerConnected(int playerId, String name) {
                notifyPlayerListUpdate();
                if (listener != null) {
                    listener.onChatMessage(name, "a rejoint la partie", true);
                }
            }

            @Override
            public void onPlayerDisconnected(int playerId) {
                notifyPlayerListUpdate();
            }

            @Override
            public void onChatMessage(int playerId, String name, String message) {
                if (listener != null) {
                    listener.onChatMessage(name, message, false);
                }
            }

            @Override
            public void onGameStart() {
                inLobby = false;
                inGame = true;
                if (listener != null) {
                    listener.onGameStart();
                }
            }

            @Override
            public void onReturnToLobby() {
                inLobby = true;
                inGame = false;
                if (listener != null) {
                    listener.onReturnToLobby();
                }
            }

            @Override
            public void onError(String error) {
                if (listener != null) {
                    listener.onError(error);
                }
            }
        });

        if (server.start()) {
            mode = NetworkMode.HOST;
            inLobby = true;
            inGame = false;

            if (listener != null) {
                listener.onModeChanged(mode);
            }
            notifyPlayerListUpdate();

            System.out.println("[NETWORK] Mode hôte activé");
            return true;
        }

        return false;
    }

    // ==================== REJOINDRE UNE PARTIE ====================

    /**
     * Démarre la recherche de serveurs LAN
     */
    public void startServerSearch() {
        if (client != null) {
            client.stopDiscovery();
        }

        client = new GameClient(GameConfig.GAME_TITLE);
        client.startDiscovery(new LANDiscovery.DiscoveryListener() {
            @Override
            public void onServerFound(LANDiscovery.ServerInfo server) {
                if (listener != null) {
                    listener.onServerFound(server);
                }
            }

            @Override
            public void onServerLost(String address) {
                if (listener != null) {
                    listener.onServerLost(address);
                }
            }
        });

        System.out.println("[NETWORK] Recherche de serveurs...");
    }

    /**
     * Arrête la recherche de serveurs
     */
    public void stopServerSearch() {
        if (client != null) {
            client.stopDiscovery();
        }
    }

    /**
     * Retourne la liste des serveurs trouvés
     */
    public Collection<LANDiscovery.ServerInfo> getDiscoveredServers() {
        return client != null ? client.getDiscoveredServers() : Collections.emptyList();
    }

    /**
     * Se connecte à un serveur
     */
    public void joinGame(String playerName, String address, int port) {
        if (mode != NetworkMode.NONE) {
            stopNetwork();
        }

        client = new GameClient(playerName);

        client.setListener(new GameClient.ClientListener() {
            @Override
            public void onConnected(int playerId, String serverName) {
                mode = NetworkMode.CLIENT;
                inLobby = true;
                inGame = false;

                if (listener != null) {
                    listener.onModeChanged(mode);
                    listener.onChatMessage("Système", "Connecté à " + serverName, true);
                }
            }

            @Override
            public void onConnectionFailed(String reason) {
                if (listener != null) {
                    listener.onError("Connexion échouée: " + reason);
                }
            }

            @Override
            public void onDisconnected(String reason) {
                mode = NetworkMode.NONE;
                inLobby = false;
                inGame = false;

                if (listener != null) {
                    listener.onDisconnected(reason);
                }
            }

            @Override
            public void onPlayerListUpdate(List<Map<String, Object>> players) {
                notifyPlayerListFromMaps(players);
            }

            @Override
            public void onChatMessage(int playerId, String playerName, String message) {
                if (listener != null) {
                    listener.onChatMessage(playerName, message, false);
                }
            }

            @Override
            public void onGameStart(long seed) {
                inLobby = false;
                inGame = true;
                if (listener != null) {
                    listener.onGameStart();
                }
            }

            @Override
            public void onReturnToLobby() {
                inLobby = true;
                inGame = false;
                if (listener != null) {
                    listener.onReturnToLobby();
                }
            }

            @Override
            public void onGameStateUpdate() {
                // Le GamePanel récupérera l'état via getPlayers/getHoles/getObstacles
            }

            @Override
            public void onPingUpdate(int ping) {
                if (listener != null) {
                    listener.onPingUpdate(ping);
                }
            }
        });

        client.connect(address, port);
    }

    // ==================== ACTIONS ====================

    /**
     * Envoie un message de chat
     */
    public void sendChat(String message) {
        if (mode == NetworkMode.HOST && server != null) {
            server.sendHostChat(message);
        } else if (mode == NetworkMode.CLIENT && client != null) {
            client.sendChatMessage(message);
        }
    }

    /**
     * Envoie une action de changement de gravité
     */
    public void sendGravitySwitch() {
        if (mode == NetworkMode.CLIENT && client != null) {
            client.sendGravitySwitch();
        }
        // En mode HOST, le serveur gère localement
    }

    /**
     * Se déclare prêt
     */
    public void setReady(boolean ready) {
        if (mode == NetworkMode.CLIENT && client != null) {
            client.setReady(ready);
        }
    }

    /**
     * Synchronise l'état autoritatif du serveur avec l'état simulé par l'hôte.
     * Appelé à chaque tick du GamePanel quand on est l'hôte.
     */
    public void syncHostState(List<factory.entity.Player> players,
                              List<factory.entity.Hole> holes,
                              List<factory.entity.Obstacle> obstacles) {
        if (mode != NetworkMode.HOST || server == null) {
            return;
        }

        // Convertir en entités réseau
        List<entity.Player> netPlayers = new ArrayList<>();
        for (factory.entity.Player p : players) {
            entity.Player np = new entity.Player(p.getPlayerId(), p.getPlayerName(), p.getPlayerColor());
            np.setX(p.getX());
            np.setY(p.getY());
            np.setVelocityY(p.getVelocityY());
            np.setGravity(entity.Gravity.valueOf(p.getGravity().name()));
            if (!p.isAlive()) {
                np.die();
            }
            np.setScore(p.getScore());
            netPlayers.add(np);
        }

        List<entity.Hole> netHoles = new ArrayList<>();
        for (factory.entity.Hole h : holes) {
            netHoles.add(new entity.Hole(h.getX(), h.getWidth()));
        }

        List<entity.Obstacle> netObstacles = new ArrayList<>();
        for (factory.entity.Obstacle o : obstacles) {
            netObstacles.add(new entity.Obstacle((int) o.getX(), (int) o.getY(), o.getWidth(), o.getHeight()));
        }

        server.updateAuthoritativeState(netPlayers, netHoles, netObstacles);
        server.broadcastGameState();
    }

    /**
     * Démarre la partie (hôte uniquement)
     */
    public void startGame() {
        if (mode == NetworkMode.HOST && server != null) {
            server.startGame();
        }
    }

    /**
     * Retourne tous les joueurs au lobby (hôte uniquement)
     */
    public void returnToLobby() {
        if (mode == NetworkMode.HOST && server != null) {
            server.returnToLobby();
            inLobby = true;
            inGame = false;
        }
    }

    /**
     * Met à jour l'état du jeu côté serveur
     * Appelé à chaque tick par le GamePanel quand on est l'hôte
     */
    public void broadcastGameState() {
        if (mode == NetworkMode.HOST && server != null && inGame) {
            server.broadcastGameState();
        }
    }

    /**
     * Arrête le réseau proprement
     */
    public void stopNetwork() {
        if (server != null) {
            server.stop();
            server = null;
        }
        if (client != null) {
            client.disconnect();
            client = null;
        }

        mode = NetworkMode.NONE;
        inLobby = false;
        inGame = false;

        if (listener != null) {
            listener.onModeChanged(mode);
        }

        System.out.println("[NETWORK] Réseau arrêté");
    }

    // ==================== HELPERS ====================

    private void notifyPlayerListUpdate() {
        if (listener == null || server == null) return;

        List<PlayerInfo> infos = new ArrayList<>();

        // L'hôte
        PlayerInfo hostInfo = new PlayerInfo();
        hostInfo.id = 0;
        hostInfo.name = server.getPlayers().get(0).getPlayerName();
        hostInfo.colorHex = colorToHex(GameConfig.NEON_CYAN);
        hostInfo.ready = true;
        hostInfo.isHost = true;
        infos.add(hostInfo);

        // Les autres joueurs
        for (Player p : server.getPlayers()) {
            if (p.getPlayerId() != 0) {
                PlayerInfo info = new PlayerInfo();
                info.id = p.getPlayerId();
                info.name = p.getPlayerName();
                info.colorHex = colorToHex(p.getPlayerColor());
                info.ready = false; // TODO: tracker l'état ready
                info.isHost = false;
                infos.add(info);
            }
        }

        listener.onPlayerListUpdate(infos);
    }

    private void notifyPlayerListFromMaps(List<Map<String, Object>> players) {
        if (listener == null) return;

        List<PlayerInfo> infos = new ArrayList<>();
        for (Map<String, Object> pm : players) {
            PlayerInfo info = new PlayerInfo();
            info.id = JsonUtils.getInt(pm, "id", 0);
            info.name = JsonUtils.getString(pm, "name", "Joueur");
            info.colorHex = JsonUtils.getString(pm, "color", "#FFFFFF");
            info.ready = JsonUtils.getBoolean(pm, "ready", false);
            info.isHost = JsonUtils.getBoolean(pm, "isHost", false);
            infos.add(info);
        }

        listener.onPlayerListUpdate(infos);
    }

    private String colorToHex(java.awt.Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    // ==================== ACCESSEURS ====================

    public void setListener(NetworkListener listener) {
        this.listener = listener;
    }

    public NetworkMode getMode() {
        return mode;
    }

    public boolean isHost() {
        return mode == NetworkMode.HOST;
    }

    public boolean isClient() {
        return mode == NetworkMode.CLIENT;
    }

    public boolean isNetworkActive() {
        return mode != NetworkMode.NONE;
    }

    public boolean isInLobby() {
        return inLobby;
    }

    public boolean isInGame() {
        return inGame;
    }

    public int getPing() {
        if (mode == NetworkMode.CLIENT && client != null) {
            return client.getPing();
        }
        return 0;
    }

    public int getPlayerCount() {
        if (mode == NetworkMode.HOST && server != null) {
            return server.getPlayerCount();
        }
        return 1;
    }

    public List<Player> getNetworkPlayers() {
        if (mode == NetworkMode.CLIENT && client != null) {
            return client.getPlayers();
        } else if (mode == NetworkMode.HOST && server != null) {
            return server.getPlayers();
        }
        return new ArrayList<>();
    }

    public List<Hole> getNetworkHoles() {
        if (mode == NetworkMode.CLIENT && client != null) {
            return client.getHoles();
        } else if (mode == NetworkMode.HOST && server != null) {
            return server.getHoles();
        }
        return new ArrayList<>();
    }

    public List<Obstacle> getNetworkObstacles() {
        if (mode == NetworkMode.CLIENT && client != null) {
            return client.getObstacles();
        } else if (mode == NetworkMode.HOST && server != null) {
            return server.getObstacles();
        }
        return new ArrayList<>();
    }
}
