package network;

import core.GameConfig;
import entity.Gravity;
import entity.Hole;
import entity.Obstacle;
import entity.Player;
import java.awt.Color;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Client de jeu réseau - Se connecte au serveur via TCP - Envoie ses inputs en
 * UDP - Reçoit l'état du jeu en UDP - Le serveur fait autorité
 */
public class GameClient {

    // Connexion
    private Socket tcpSocket;
    private BufferedReader tcpReader;
    private PrintWriter tcpWriter;
    private DatagramSocket udpSocket;

    // Identification
    private String playerName;
    private int playerId = -1;
    private String serverAddress;

    // État
    private volatile boolean connected;
    private volatile boolean running;
    private long inputSequence = 0;

    // Ping
    private long lastPingTime;
    private int currentPing;

    // État du jeu reçu du serveur
    private final List<Player> players = new ArrayList<>();
    private final List<Hole> holes = new ArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>();
    private long lastTick = 0;

    // Découverte LAN
    private LANDiscovery lanDiscovery;

    // Callbacks
    private ClientListener listener;

    /**
     * Interface de callback pour les événements client
     */
    public interface ClientListener {

        void onConnected(int playerId, String serverName);

        void onConnectionFailed(String reason);

        void onDisconnected(String reason);

        void onPlayerListUpdate(List<Map<String, Object>> players);

        void onChatMessage(int playerId, String playerName, String message);

        void onGameStart(long seed);

        void onReturnToLobby();

        void onGameStateUpdate();

        void onPingUpdate(int ping);
    }

    public GameClient(String playerName) {
        this.playerName = playerName;
    }

    // ==================== DÉCOUVERTE LAN ====================
    /**
     * Démarre la recherche de serveurs sur le LAN
     */
    public void startDiscovery(LANDiscovery.DiscoveryListener discoveryListener) {
        if (lanDiscovery != null) {
            lanDiscovery.stop();
        }
        lanDiscovery = new LANDiscovery();
        lanDiscovery.startClientDiscovery(discoveryListener);
    }

    /**
     * Arrête la recherche de serveurs
     */
    public void stopDiscovery() {
        if (lanDiscovery != null) {
            lanDiscovery.stop();
            lanDiscovery = null;
        }
    }

    /**
     * Retourne les serveurs découverts
     */
    public Collection<LANDiscovery.ServerInfo> getDiscoveredServers() {
        return lanDiscovery != null ? lanDiscovery.getDiscoveredServers() : Collections.emptyList();
    }

    // ==================== CONNEXION ====================
    /**
     * Se connecte à un serveur
     */
    public void connect(String address, int port) {
        this.serverAddress = address;

        new Thread(() -> {
            try {
                // Connexion TCP
                tcpSocket = new Socket();
                tcpSocket.connect(new InetSocketAddress(address, port), NetworkProtocol.CONNECTION_TIMEOUT);
                tcpSocket.setSoTimeout(0); // Pas de timeout pour la lecture

                tcpReader = new BufferedReader(
                        new InputStreamReader(tcpSocket.getInputStream(), "UTF-8")
                );
                tcpWriter = new PrintWriter(
                        new OutputStreamWriter(tcpSocket.getOutputStream(), "UTF-8"), true
                );

                // Envoyer la demande de connexion
                tcpWriter.println(JsonUtils.builder()
                        .put("type", NetworkProtocol.TcpMessageType.CONNECT_REQUEST.name())
                        .put("playerName", playerName)
                        .put("version", GameConfig.GAME_VERSION)
                        .build());

                // Attendre la réponse
                String response = tcpReader.readLine();
                Map<String, Object> resp = JsonUtils.parse(response);
                String type = JsonUtils.getString(resp, "type", "");

                if (NetworkProtocol.TcpMessageType.CONNECT_ACCEPT.name().equals(type)) {
                    playerId = JsonUtils.getInt(resp, "playerId", -1);
                    String serverName = JsonUtils.getString(resp, "serverName", "Serveur");

                    connected = true;
                    running = true;

                    // Ouvrir le socket UDP
                    udpSocket = new DatagramSocket();
                    udpSocket.setSoTimeout(100);

                    // Envoyer un premier paquet UDP pour établir le port
                    sendInput("NONE");

                    // Démarrer les threads de lecture
                    new Thread(this::tcpReadLoop, "Client-TCP").start();
                    new Thread(this::udpReadLoop, "Client-UDP").start();
                    new Thread(this::pingLoop, "Client-Ping").start();

                    // Arrêter la découverte
                    stopDiscovery();

                    if (listener != null) {
                        listener.onConnected(playerId, serverName);
                        listener.onPlayerListUpdate(JsonUtils.getArray(resp, "players")
                                .stream()
                                .filter(o -> o instanceof Map)
                                .map(o -> {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> m = (Map<String, Object>) o;
                                    return m;
                                })
                                .collect(java.util.stream.Collectors.toList()));
                    }

                    System.out.println("[CLIENT] Connecté au serveur (id=" + playerId + ")");

                } else if (NetworkProtocol.TcpMessageType.CONNECT_REJECT.name().equals(type)) {
                    String reason = JsonUtils.getString(resp, "reason", "Connexion refusée");
                    tcpSocket.close();
                    if (listener != null) {
                        listener.onConnectionFailed(reason);
                    }
                }

            } catch (SocketTimeoutException e) {
                if (listener != null) {
                    listener.onConnectionFailed("Timeout: serveur injoignable");
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onConnectionFailed("Erreur: " + e.getMessage());
                }
            }
        }, "Client-Connect").start();
    }

    /**
     * Se déconnecte du serveur
     */
    public void disconnect() {
        if (!connected) {
            return;
        }

        try {
            tcpWriter.println(JsonUtils.builder()
                    .put("type", NetworkProtocol.TcpMessageType.DISCONNECT.name())
                    .build());
        } catch (Exception ignored) {
        }

        cleanup("Déconnexion");
    }

    private void cleanup(String reason) {
        connected = false;
        running = false;

        try {
            if (tcpSocket != null) {
                tcpSocket.close();
            }
            if (udpSocket != null) {
                udpSocket.close();
            }
        } catch (Exception ignored) {
        }

        if (listener != null && reason != null) {
            listener.onDisconnected(reason);
        }

        System.out.println("[CLIENT] Déconnecté: " + reason);
    }

    // ==================== RÉCEPTION TCP ====================
    private void tcpReadLoop() {
        try {
            String line;
            while (running && (line = tcpReader.readLine()) != null) {
                processTcpMessage(line);
            }
        } catch (Exception e) {
            if (running) {
                cleanup("Connexion perdue");
            }
        }
    }

    private void processTcpMessage(String json) {
        try {
            Map<String, Object> msg = JsonUtils.parse(json);
            String type = JsonUtils.getString(msg, "type", "");

            switch (type) {
                case "PLAYER_LIST":
                    if (listener != null) {
                        List<Map<String, Object>> playerList = JsonUtils.getArray(msg, "players")
                                .stream()
                                .filter(o -> o instanceof Map)
                                .map(o -> {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> m = (Map<String, Object>) o;
                                    return m;
                                })
                                .collect(java.util.stream.Collectors.toList());
                        listener.onPlayerListUpdate(playerList);
                    }
                    break;

                case "CHAT_MESSAGE":
                    if (listener != null) {
                        int senderId = JsonUtils.getInt(msg, "playerId", -1);
                        String senderName = JsonUtils.getString(msg, "playerName", "");
                        String message = JsonUtils.getString(msg, "message", "");
                        listener.onChatMessage(senderId, senderName, message);
                    }
                    break;

                case "GAME_START":
                    long seed = (long) JsonUtils.getDouble(msg, "seed", 0);
                    if (listener != null) {
                        listener.onGameStart(seed);
                    }
                    break;

                case "RETURN_TO_LOBBY":
                    if (listener != null) {
                        listener.onReturnToLobby();
                    }
                    break;

                case "PONG":
                    long timestamp = (long) JsonUtils.getDouble(msg, "timestamp", 0);
                    currentPing = (int) (System.currentTimeMillis() - timestamp);
                    if (listener != null) {
                        listener.onPingUpdate(currentPing);
                    }
                    break;

                case "DISCONNECT":
                    String reason = JsonUtils.getString(msg, "reason", "Serveur déconnecté");
                    cleanup(reason);
                    break;
            }
        } catch (Exception e) {
            System.err.println("[CLIENT] Erreur traitement TCP: " + e.getMessage());
        }
    }

    // ==================== RÉCEPTION UDP ====================
    private void udpReadLoop() {
        byte[] buffer = new byte[2048];

        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);

                String json = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
                processUdpMessage(json);

            } catch (SocketTimeoutException e) {
                // Normal
            } catch (Exception e) {
                if (running) {
                    System.err.println("[CLIENT] Erreur UDP: " + e.getMessage());
                }
            }
        }
    }

    private void processUdpMessage(String json) {
        try {
            Map<String, Object> msg = JsonUtils.parse(json);
            String type = JsonUtils.getString(msg, "type", "");

            if ("GAME_STATE".equals(type)) {
                long tick = (long) JsonUtils.getDouble(msg, "tick", 0);

                // Ignorer les états plus vieux
                if (tick <= lastTick) {
                    return;
                }
                lastTick = tick;

                // Mettre à jour les joueurs
                synchronized (players) {
                    players.clear();
                    for (Object obj : JsonUtils.getArray(msg, "players")) {
                        if (obj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> ps = (Map<String, Object>) obj;

                            int id = JsonUtils.getInt(ps, "id", 0);
                            double x = JsonUtils.getDouble(ps, "x", 0);
                            double y = JsonUtils.getDouble(ps, "y", 0);
                            double vy = JsonUtils.getDouble(ps, "vy", 0);
                            String gravStr = JsonUtils.getString(ps, "gravity", "DOWN");
                            boolean alive = JsonUtils.getBoolean(ps, "alive", true);
                            int score = JsonUtils.getInt(ps, "score", 0);

                            String name = JsonUtils.getString(ps, "name", "Player" + id);
                            String colorHex = JsonUtils.getString(ps, "color", null);
                            Color color = colorHex != null ? Color.decode(colorHex) : getPlayerColor(id);

                            Player p = new Player(id, name, color);
                            p.setX((int) x);
                            p.setY((int) y);
                            p.setVelocityY(vy);
                            p.setGravity(Gravity.valueOf(gravStr));
                            if (!alive) {
                                p.die();
                            }
                            p.setScore(score);
                            players.add(p);
                        }
                    }
                }

                // Mettre à jour les trous
                synchronized (holes) {
                    holes.clear();
                    for (Object obj : JsonUtils.getArray(msg, "holes")) {
                        if (obj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> hs = (Map<String, Object>) obj;

                            int hx = JsonUtils.getInt(hs, "x", 0);
                            int width = JsonUtils.getInt(hs, "width", 80);
                            holes.add(new Hole(hx, width));
                        }
                    }
                }

                // Mettre à jour les obstacles
                synchronized (obstacles) {
                    obstacles.clear();
                    for (Object obj : JsonUtils.getArray(msg, "obstacles")) {
                        if (obj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> os = (Map<String, Object>) obj;

                            int ox = JsonUtils.getInt(os, "x", 0);
                            int oy = JsonUtils.getInt(os, "y", 0);
                            int ow = JsonUtils.getInt(os, "width", 40);
                            int oh = JsonUtils.getInt(os, "height", 100);
                            obstacles.add(new Obstacle(ox, oy, ow, oh));
                        }
                    }
                }

                if (listener != null) {
                    listener.onGameStateUpdate();
                }
            }
        } catch (Exception e) {
            // Ignorer les paquets malformés
        }
    }

    private Color getPlayerColor(int id) {
        Color[] colors = {
            GameConfig.NEON_CYAN,
            GameConfig.NEON_PINK,
            GameConfig.NEON_PURPLE,
            GameConfig.NEON_GREEN
        };
        return colors[id % colors.length];
    }

    // ==================== ENVOI ====================
    /**
     * Envoie une action au serveur (UDP)
     */
    public void sendInput(String action) {
        if (!connected || udpSocket == null) {
            return;
        }

        try {
            String json = JsonUtils.builder()
                    .put("type", "INPUT")
                    .put("playerId", playerId)
                    .put("sequence", ++inputSequence)
                    .put("action", action)
                    .put("timestamp", System.currentTimeMillis())
                    .build();

            byte[] data = json.getBytes("UTF-8");
            DatagramPacket packet = new DatagramPacket(
                    data, data.length,
                    InetAddress.getByName(serverAddress),
                    NetworkProtocol.UDP_PORT
            );
            udpSocket.send(packet);

        } catch (Exception e) {
            System.err.println("[CLIENT] Erreur envoi input: " + e.getMessage());
        }
    }

    /**
     * Envoie une action de changement de gravité
     */
    public void sendGravitySwitch() {
        sendInput("GRAVITY_SWITCH");
    }

    /**
     * Envoie un message de chat (TCP)
     */
    public void sendChatMessage(String message) {
        if (!connected) {
            return;
        }

        tcpWriter.println(JsonUtils.builder()
                .put("type", NetworkProtocol.TcpMessageType.CHAT_MESSAGE.name())
                .put("message", message)
                .build());
    }

    /**
     * Se déclare prêt (TCP)
     */
    public void setReady(boolean ready) {
        if (!connected) {
            return;
        }

        tcpWriter.println(JsonUtils.builder()
                .put("type", NetworkProtocol.TcpMessageType.PLAYER_READY.name())
                .put("ready", ready)
                .build());
    }

    // ==================== PING ====================
    private void pingLoop() {
        while (running) {
            try {
                Thread.sleep(1000);

                if (connected) {
                    lastPingTime = System.currentTimeMillis();
                    tcpWriter.println(JsonUtils.builder()
                            .put("type", "PING")
                            .put("timestamp", lastPingTime)
                            .build());
                }
            } catch (Exception e) {
                if (running) {
                    System.err.println("[CLIENT] Erreur ping: " + e.getMessage());
                }
            }
        }
    }

    // ==================== ACCESSEURS ====================
    public void setListener(ClientListener listener) {
        this.listener = listener;
    }

    public boolean isConnected() {
        return connected;
    }

    public int getPlayerId() {
        return playerId;
    }

    public int getPing() {
        return currentPing;
    }

    public List<Player> getPlayers() {
        synchronized (players) {
            return new ArrayList<>(players);
        }
    }

    public List<Hole> getHoles() {
        synchronized (holes) {
            return new ArrayList<>(holes);
        }
    }

    public List<Obstacle> getObstacles() {
        synchronized (obstacles) {
            return new ArrayList<>(obstacles);
        }
    }
}
