package network;

import core.GameConfig;
import entity.Hole;
import entity.Obstacle;
import entity.Player;
import java.awt.Color;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Serveur de jeu autoritaire - Gère les connexions TCP des clients - Reçoit les
 * inputs UDP des clients - Diffuse l'état du jeu en UDP - Le serveur fait
 * autorité sur l'état du jeu
 */
public class GameServer {

    // Sockets
    private ServerSocket tcpServer;
    private DatagramSocket udpSocket;

    // État
    private volatile boolean running;
    private String serverName;
    private final Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private int nextPlayerId = 0;
    private long currentTick = 0;

    // Découverte LAN
    private LANDiscovery lanDiscovery;

    // État du jeu (le serveur est autoritaire)
    private boolean gameStarted;
    private final List<Player> players = new ArrayList<>();
    private final List<Hole> holes = new ArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>();
    private long gameSeed;

    // Callbacks
    private ServerListener listener;

    // Couleurs disponibles pour les joueurs
    private static final Color[] PLAYER_COLORS = {
        GameConfig.NEON_CYAN,
        GameConfig.NEON_PINK,
        GameConfig.NEON_PURPLE,
        GameConfig.NEON_GREEN
    };

    /**
     * Interface de callback pour les événements serveur
     */
    public interface ServerListener {

        void onPlayerConnected(int playerId, String playerName);

        void onPlayerDisconnected(int playerId);

        void onChatMessage(int playerId, String playerName, String message);

        void onGameStart();

        void onReturnToLobby();

        void onError(String error);
    }

    public GameServer(String serverName) {
        this.serverName = serverName;
    }

    // ==================== DÉMARRAGE/ARRÊT ====================
    /**
     * Démarre le serveur
     */
    public boolean start() {
        try {
            // Démarrer le serveur TCP
            tcpServer = new ServerSocket(NetworkProtocol.TCP_PORT);
            tcpServer.setSoTimeout(1000); // Pour pouvoir arrêter proprement

            // Démarrer le socket UDP
            udpSocket = new DatagramSocket(NetworkProtocol.UDP_PORT);
            udpSocket.setSoTimeout(100);

            running = true;
            gameStarted = false;

            // Thread d'acceptation TCP
            new Thread(this::acceptLoop, "Server-Accept").start();

            // Thread de réception UDP
            new Thread(this::udpReceiveLoop, "Server-UDP-Recv").start();

            // Démarrer l'annonce LAN
            lanDiscovery = new LANDiscovery();
            lanDiscovery.startServerBroadcast(serverName);

            // Ajouter le joueur hôte (id=0)
            addHostPlayer();

            System.out.println("[SERVER] Serveur démarré sur le port " + NetworkProtocol.TCP_PORT);
            return true;

        } catch (Exception e) {
            System.err.println("[SERVER] Erreur démarrage: " + e.getMessage());
            if (listener != null) {
                listener.onError("Impossible de démarrer le serveur: " + e.getMessage());
            }
            stop();
            return false;
        }
    }

    /**
     * Arrête le serveur
     */
    public void stop() {
        running = false;

        // Notifier tous les clients
        broadcastTcp(JsonUtils.builder()
                .put("type", NetworkProtocol.TcpMessageType.DISCONNECT.name())
                .put("reason", "Serveur fermé")
                .build());

        // Fermer toutes les connexions
        for (ClientHandler client : clients.values()) {
            client.close();
        }
        clients.clear();

        // Fermer les sockets
        try {
            if (tcpServer != null) {
                tcpServer.close();
            }
            if (udpSocket != null) {
                udpSocket.close();
            }
        } catch (Exception ignored) {
        }

        // Arrêter la découverte LAN
        if (lanDiscovery != null) {
            lanDiscovery.stop();
        }

        System.out.println("[SERVER] Serveur arrêté");
    }

    // ==================== GESTION DES CONNEXIONS ====================
    private void acceptLoop() {
        while (running) {
            try {
                Socket clientSocket = tcpServer.accept();
                handleNewConnection(clientSocket);
            } catch (SocketTimeoutException e) {
                // Normal, permet de vérifier running
            } catch (Exception e) {
                if (running) {
                    System.err.println("[SERVER] Erreur accept: " + e.getMessage());
                }
            }
        }
    }

    private void handleNewConnection(Socket socket) {
        try {
            // Lire la demande de connexion
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), "UTF-8")
            );
            PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true
            );

            String requestJson = reader.readLine();
            Map<String, Object> request = JsonUtils.parse(requestJson);

            String type = JsonUtils.getString(request, "type", "");
            if (!NetworkProtocol.TcpMessageType.CONNECT_REQUEST.name().equals(type)) {
                socket.close();
                return;
            }

            String playerName = JsonUtils.getString(request, "playerName", "Joueur");

            // Vérifier si la partie est pleine
            if (clients.size() >= NetworkProtocol.MAX_PLAYERS - 1) { // -1 car l'hôte compte
                writer.println(JsonUtils.builder()
                        .put("type", NetworkProtocol.TcpMessageType.CONNECT_REJECT.name())
                        .put("reason", "Partie pleine")
                        .build());
                socket.close();
                return;
            }

            // Vérifier si la partie a commencé
            if (gameStarted) {
                writer.println(JsonUtils.builder()
                        .put("type", NetworkProtocol.TcpMessageType.CONNECT_REJECT.name())
                        .put("reason", "Partie déjà en cours")
                        .build());
                socket.close();
                return;
            }

            // Accepter la connexion
            int playerId = ++nextPlayerId;

            // Créer le joueur
            Player newPlayer = new Player(playerId, playerName, PLAYER_COLORS[playerId % PLAYER_COLORS.length]);
            synchronized (players) {
                players.add(newPlayer);
            }

            // Créer le handler client
            ClientHandler handler = new ClientHandler(socket, playerId, playerName, reader, writer);
            clients.put(playerId, handler);

            // Envoyer l'acceptation
            writer.println(JsonUtils.builder()
                    .put("type", NetworkProtocol.TcpMessageType.CONNECT_ACCEPT.name())
                    .put("playerId", playerId)
                    .put("serverName", serverName)
                    .putArray("players", buildPlayerList())
                    .build());

            // Démarrer le thread de lecture
            new Thread(() -> clientReadLoop(handler), "Client-" + playerId).start();

            // Notifier tous les clients de la nouvelle liste
            broadcastPlayerList();

            // Mettre à jour l'annonce LAN
            lanDiscovery.updateServerInfo(clients.size() + 1, gameStarted);

            if (listener != null) {
                listener.onPlayerConnected(playerId, playerName);
            }

            System.out.println("[SERVER] Joueur connecté: " + playerName + " (id=" + playerId + ")");

        } catch (Exception e) {
            System.err.println("[SERVER] Erreur nouvelle connexion: " + e.getMessage());
            try {
                socket.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void clientReadLoop(ClientHandler handler) {
        try {
            String line;
            while (running && (line = handler.reader.readLine()) != null) {
                processClientMessage(handler, line);
            }
        } catch (Exception e) {
            if (running) {
                System.err.println("[SERVER] Erreur lecture client " + handler.playerId + ": " + e.getMessage());
            }
        } finally {
            disconnectClient(handler.playerId);
        }
    }

    private void processClientMessage(ClientHandler handler, String json) {
        try {
            Map<String, Object> msg = JsonUtils.parse(json);
            String type = JsonUtils.getString(msg, "type", "");

            switch (type) {
                case "CHAT_MESSAGE":
                    String message = JsonUtils.getString(msg, "message", "");
                    broadcastChat(handler.playerId, handler.playerName, message);
                    if (listener != null) {
                        listener.onChatMessage(handler.playerId, handler.playerName, message);
                    }
                    break;

                case "PLAYER_READY":
                    boolean ready = JsonUtils.getBoolean(msg, "ready", false);
                    handler.ready = ready;
                    broadcastPlayerList();
                    break;

                case "PING":
                    long timestamp = JsonUtils.getInt(msg, "timestamp", 0);
                    handler.writer.println(JsonUtils.builder()
                            .put("type", "PONG")
                            .put("timestamp", timestamp)
                            .build());
                    break;

                case "DISCONNECT":
                    disconnectClient(handler.playerId);
                    break;
            }
        } catch (Exception e) {
            System.err.println("[SERVER] Erreur traitement message: " + e.getMessage());
        }
    }

    private void disconnectClient(int playerId) {
        ClientHandler handler = clients.remove(playerId);
        if (handler != null) {
            handler.close();

            // Retirer le joueur
            synchronized (players) {
                players.removeIf(p -> p.getPlayerId() == playerId);
            }

            broadcastPlayerList();
            lanDiscovery.updateServerInfo(clients.size() + 1, gameStarted);

            if (listener != null) {
                listener.onPlayerDisconnected(playerId);
            }

            System.out.println("[SERVER] Joueur déconnecté: " + handler.playerName);

            // Si la partie est en cours et il n'y a plus de clients connectés
            // (seulement l'hôte reste), arrêter la partie
            if (gameStarted && clients.isEmpty()) {
                System.out.println("[SERVER] Plus aucun joueur connecté, arrêt de la partie");
                // Notifier via le listener que la partie doit s'arrêter
                if (listener != null) {
                    listener.onError("Tous les joueurs ont quitté la partie");
                }
            }
        }
    }

    // ==================== UDP ====================
    private void udpReceiveLoop() {
        byte[] buffer = new byte[1024];

        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);

                String json = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
                processUdpMessage(json, packet.getAddress(), packet.getPort());

            } catch (SocketTimeoutException e) {
                // Normal
            } catch (Exception e) {
                if (running) {
                    System.err.println("[SERVER] Erreur UDP: " + e.getMessage());
                }
            }
        }
    }

    private void processUdpMessage(String json, InetAddress address, int port) {
        try {
            Map<String, Object> msg = JsonUtils.parse(json);
            String type = JsonUtils.getString(msg, "type", "");

            if ("INPUT".equals(type)) {
                int playerId = JsonUtils.getInt(msg, "playerId", -1);
                String action = JsonUtils.getString(msg, "action", "NONE");

                // Enregistrer l'adresse UDP du client
                ClientHandler handler = clients.get(playerId);
                if (handler != null) {
                    handler.udpAddress = address;
                    handler.udpPort = port;
                }

                // Appliquer l'action
                if ("GRAVITY_SWITCH".equals(action)) {
                    synchronized (players) {
                        for (Player p : players) {
                            if (p.getPlayerId() == playerId && p.isAlive()) {
                                p.switchGravity();
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignorer les paquets malformés
        }
    }

    /**
     * Diffuse l'état du jeu à tous les clients en UDP
     */
    public void broadcastGameState() {
        if (!running || !gameStarted) {
            return;
        }

        currentTick++;

        // Construire l'état du jeu
        List<Map<String, Object>> playerStates = new ArrayList<>();
        synchronized (players) {
            for (Player p : players) {
                Map<String, Object> ps = new LinkedHashMap<>();
                ps.put("id", p.getPlayerId());
                ps.put("x", p.getX());
                ps.put("y", p.getY());
                ps.put("vy", p.getVelocityY());
                ps.put("gravity", p.getGravity().name());
                ps.put("alive", p.isAlive());
                ps.put("score", p.getScore());
                playerStates.add(ps);
            }
        }

        List<Map<String, Object>> holeStates = new ArrayList<>();
        synchronized (holes) {
            for (Hole h : holes) {
                Map<String, Object> hs = new LinkedHashMap<>();
                hs.put("x", h.getX());
                hs.put("width", h.getWidth());
                holeStates.add(hs);
            }
        }

        List<Map<String, Object>> obstacleStates = new ArrayList<>();
        synchronized (obstacles) {
            for (Obstacle o : obstacles) {
                Map<String, Object> os = new LinkedHashMap<>();
                os.put("x", o.getX());
                os.put("y", o.getY());
                os.put("width", o.getWidth());
                os.put("height", o.getHeight());
                obstacleStates.add(os);
            }
        }

        String json = JsonUtils.builder()
                .put("type", "GAME_STATE")
                .put("tick", currentTick)
                .putArray("players", playerStates)
                .putArray("holes", holeStates)
                .putArray("obstacles", obstacleStates)
                .build();

        byte[] data;
        try {
            data = json.getBytes("UTF-8");
        } catch (Exception e) {
            return;
        }

        // Envoyer à tous les clients
        for (ClientHandler handler : clients.values()) {
            if (handler.udpAddress != null) {
                try {
                    DatagramPacket packet = new DatagramPacket(
                            data, data.length, handler.udpAddress, handler.udpPort
                    );
                    udpSocket.send(packet);
                } catch (Exception e) {
                    // Client peut être déconnecté
                }
            }
        }
    }

    // ==================== MESSAGES TCP ====================
    private void broadcastTcp(String json) {
        for (ClientHandler handler : clients.values()) {
            handler.writer.println(json);
        }
    }

    private void broadcastPlayerList() {
        String json = JsonUtils.builder()
                .put("type", NetworkProtocol.TcpMessageType.PLAYER_LIST.name())
                .putArray("players", buildPlayerList())
                .build();
        broadcastTcp(json);
    }

    private void broadcastChat(int senderId, String senderName, String message) {
        String json = JsonUtils.builder()
                .put("type", NetworkProtocol.TcpMessageType.CHAT_MESSAGE.name())
                .put("playerId", senderId)
                .put("playerName", senderName)
                .put("message", message)
                .put("timestamp", System.currentTimeMillis())
                .build();
        broadcastTcp(json);
    }

    private List<Map<String, Object>> buildPlayerList() {
        List<Map<String, Object>> list = new ArrayList<>();

        // Ajouter l'hôte (id=0)
        Map<String, Object> host = new LinkedHashMap<>();
        host.put("id", 0);
        host.put("name", serverName.replace("Partie de ", ""));
        host.put("color", colorToHex(PLAYER_COLORS[0]));
        host.put("ready", true); // L'hôte est toujours prêt
        host.put("isHost", true);
        list.add(host);

        // Ajouter les clients
        for (ClientHandler handler : clients.values()) {
            Map<String, Object> player = new LinkedHashMap<>();
            player.put("id", handler.playerId);
            player.put("name", handler.playerName);
            player.put("color", colorToHex(PLAYER_COLORS[handler.playerId % PLAYER_COLORS.length]));
            player.put("ready", handler.ready);
            player.put("isHost", false);
            list.add(player);
        }

        return list;
    }

    private String colorToHex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    // ==================== CONTRÔLE DU JEU ====================
    private void addHostPlayer() {
        // Le joueur hôte a l'id 0
        String hostName = serverName.replace("Partie de ", "");
        Player hostPlayer = new Player(0, hostName, PLAYER_COLORS[0]);
        synchronized (players) {
            players.add(hostPlayer);
        }
    }

    /**
     * Démarre la partie
     */
    public void startGame() {
        if (gameStarted) {
            return;
        }

        gameStarted = true;
        gameSeed = System.currentTimeMillis();

        // Notifier tous les clients
        String json = JsonUtils.builder()
                .put("type", NetworkProtocol.TcpMessageType.GAME_START.name())
                .put("seed", gameSeed)
                .put("countdown", 3)
                .build();
        broadcastTcp(json);

        lanDiscovery.updateServerInfo(clients.size() + 1, true);

        if (listener != null) {
            listener.onGameStart();
        }

        System.out.println("[SERVER] Partie démarrée (seed=" + gameSeed + ")");
    }

    /**
     * Retourne tous les joueurs au lobby
     */
    public void returnToLobby() {
        if (!running) {
            return;
        }

        gameStarted = false;

        // Reset des joueurs
        synchronized (players) {
            for (Player p : players) {
                p.reset();
            }
        }

        // Vider les obstacles et trous
        synchronized (holes) {
            holes.clear();
        }
        synchronized (obstacles) {
            obstacles.clear();
        }

        // Notifier tous les clients
        String json = JsonUtils.builder()
                .put("type", "RETURN_TO_LOBBY")
                .put("message", "L'hôte a renvoyé tout le monde au lobby")
                .build();
        broadcastTcp(json);

        // Renvoyer la liste des joueurs
        broadcastPlayerList();

        lanDiscovery.updateServerInfo(clients.size() + 1, false);

        // Notifier le listener local (pour l'hôte)
        if (listener != null) {
            listener.onReturnToLobby();
        }

        System.out.println("[SERVER] Retour au lobby");
    }

    /**
     * Envoie un message chat depuis l'hôte
     */
    public void sendHostChat(String message) {
        String hostName = serverName.replace("Partie de ", "");
        broadcastChat(0, hostName, message);
    }

    // ==================== ACCESSEURS ====================
    public void setListener(ServerListener listener) {
        this.listener = listener;
    }

    public List<Player> getPlayers() {
        synchronized (players) {
            return new ArrayList<>(players);
        }
    }

    public List<Hole> getHoles() {
        return holes;
    }

    public List<Obstacle> getObstacles() {
        return obstacles;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public int getPlayerCount() {
        return clients.size() + 1; // +1 pour l'hôte
    }

    // ==================== CLASSE INTERNE ====================
    private static class ClientHandler {

        final Socket socket;
        final int playerId;
        final String playerName;
        final BufferedReader reader;
        final PrintWriter writer;
        boolean ready = false;
        InetAddress udpAddress;
        int udpPort;

        ClientHandler(Socket socket, int playerId, String playerName,
                BufferedReader reader, PrintWriter writer) {
            this.socket = socket;
            this.playerId = playerId;
            this.playerName = playerName;
            this.reader = reader;
            this.writer = writer;
        }

        void close() {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
        }
    }
}
