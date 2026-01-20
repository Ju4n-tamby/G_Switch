package network;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Découverte automatique de serveurs sur le réseau local (LAN)
 * Utilise des broadcasts UDP pour annoncer/découvrir les parties
 */
public class LANDiscovery {
    
    private DatagramSocket socket;
    private volatile boolean running;
    private Thread discoveryThread;
    
    // Pour le serveur: annonce sa présence
    private boolean isServer;
    private String serverName;
    private int playerCount;
    private int maxPlayers;
    private boolean inGame;
    
    // Pour les clients: liste des serveurs trouvés
    private final Map<String, ServerInfo> discoveredServers = new ConcurrentHashMap<>();
    private DiscoveryListener listener;
    
    /**
     * Informations sur un serveur découvert
     */
    public static class ServerInfo {
        public String address;
        public int tcpPort;
        public String serverName;
        public int playerCount;
        public int maxPlayers;
        public boolean inGame;
        public long lastSeen;
        
        @Override
        public String toString() {
            return serverName + " (" + playerCount + "/" + maxPlayers + ")" + 
                   (inGame ? " [En cours]" : " [En attente]");
        }
    }
    
    /**
     * Interface de callback pour les événements de découverte
     */
    public interface DiscoveryListener {
        void onServerFound(ServerInfo server);
        void onServerLost(String address);
    }
    
    public LANDiscovery() {
        this.maxPlayers = NetworkProtocol.MAX_PLAYERS;
    }
    
    // ==================== MODE SERVEUR ====================
    
    /**
     * Démarre l'annonce du serveur sur le LAN
     */
    public void startServerBroadcast(String serverName) {
        this.isServer = true;
        this.serverName = serverName;
        this.playerCount = 1;
        this.inGame = false;
        
        running = true;
        discoveryThread = new Thread(this::serverBroadcastLoop, "LAN-Broadcast");
        discoveryThread.setDaemon(true);
        discoveryThread.start();
        
        System.out.println("[LAN] Annonce serveur démarrée: " + serverName);
    }
    
    private void serverBroadcastLoop() {
        try {
            socket = new DatagramSocket();
            socket.setBroadcast(true);
            
            while (running) {
                try {
                    // Construire le message de découverte
                    String json = JsonUtils.builder()
                        .put("magic", NetworkProtocol.DISCOVERY_MAGIC)
                        .put("serverName", serverName)
                        .put("playerCount", playerCount)
                        .put("maxPlayers", maxPlayers)
                        .put("tcpPort", NetworkProtocol.TCP_PORT)
                        .put("inGame", inGame)
                        .build();
                    
                    byte[] data = json.getBytes("UTF-8");
                    
                    // Envoyer sur l'adresse broadcast
                    InetAddress broadcast = InetAddress.getByName("255.255.255.255");
                    DatagramPacket packet = new DatagramPacket(
                        data, data.length, broadcast, NetworkProtocol.DISCOVERY_PORT
                    );
                    socket.send(packet);
                    
                    // Aussi envoyer sur les broadcasts de chaque interface
                    broadcastToAllInterfaces(data);
                    
                } catch (Exception e) {
                    if (running) {
                        System.err.println("[LAN] Erreur broadcast: " + e.getMessage());
                    }
                }
                
                Thread.sleep(NetworkProtocol.DISCOVERY_INTERVAL);
            }
        } catch (Exception e) {
            System.err.println("[LAN] Erreur serveur broadcast: " + e.getMessage());
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }
    
    private void broadcastToAllInterfaces(byte[] data) throws Exception {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface ni = interfaces.nextElement();
            if (ni.isLoopback() || !ni.isUp()) continue;
            
            for (InterfaceAddress addr : ni.getInterfaceAddresses()) {
                InetAddress broadcast = addr.getBroadcast();
                if (broadcast != null) {
                    try {
                        DatagramPacket packet = new DatagramPacket(
                            data, data.length, broadcast, NetworkProtocol.DISCOVERY_PORT
                        );
                        socket.send(packet);
                    } catch (Exception ignored) {}
                }
            }
        }
    }
    
    public void updateServerInfo(int playerCount, boolean inGame) {
        this.playerCount = playerCount;
        this.inGame = inGame;
    }
    
    // ==================== MODE CLIENT ====================
    
    /**
     * Démarre l'écoute des serveurs sur le LAN
     */
    public void startClientDiscovery(DiscoveryListener listener) {
        this.isServer = false;
        this.listener = listener;
        
        running = true;
        discoveryThread = new Thread(this::clientDiscoveryLoop, "LAN-Discovery");
        discoveryThread.setDaemon(true);
        discoveryThread.start();
        
        System.out.println("[LAN] Recherche de serveurs démarrée...");
    }
    
    private void clientDiscoveryLoop() {
        try {
            socket = new DatagramSocket(NetworkProtocol.DISCOVERY_PORT);
            socket.setSoTimeout(1000); // Timeout pour vérifier running régulièrement
            
            byte[] buffer = new byte[1024];
            
            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    
                    String json = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
                    processDiscoveryPacket(json, packet.getAddress().getHostAddress());
                    
                } catch (SocketTimeoutException e) {
                    // Normal, permet de vérifier running
                    cleanupOldServers();
                }
            }
        } catch (BindException e) {
            System.err.println("[LAN] Port " + NetworkProtocol.DISCOVERY_PORT + " déjà utilisé");
        } catch (Exception e) {
            if (running) {
                System.err.println("[LAN] Erreur découverte: " + e.getMessage());
            }
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }
    
    private void processDiscoveryPacket(String json, String sourceAddress) {
        try {
            Map<String, Object> data = JsonUtils.parse(json);
            
            String magic = JsonUtils.getString(data, "magic", "");
            if (!NetworkProtocol.DISCOVERY_MAGIC.equals(magic)) {
                return; // Pas notre protocole
            }
            
            ServerInfo info = new ServerInfo();
            info.address = sourceAddress;
            info.tcpPort = JsonUtils.getInt(data, "tcpPort", NetworkProtocol.TCP_PORT);
            info.serverName = JsonUtils.getString(data, "serverName", "Serveur inconnu");
            info.playerCount = JsonUtils.getInt(data, "playerCount", 1);
            info.maxPlayers = JsonUtils.getInt(data, "maxPlayers", NetworkProtocol.MAX_PLAYERS);
            info.inGame = JsonUtils.getBoolean(data, "inGame", false);
            info.lastSeen = System.currentTimeMillis();
            
            String key = sourceAddress + ":" + info.tcpPort;
            boolean isNew = !discoveredServers.containsKey(key);
            discoveredServers.put(key, info);
            
            if (isNew && listener != null) {
                listener.onServerFound(info);
            }
            
        } catch (Exception e) {
            // Paquet malformé, ignorer
        }
    }
    
    private void cleanupOldServers() {
        long now = System.currentTimeMillis();
        long timeout = NetworkProtocol.DISCOVERY_INTERVAL * 3; // 3 cycles sans réponse = mort
        
        Iterator<Map.Entry<String, ServerInfo>> it = discoveredServers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ServerInfo> entry = it.next();
            if (now - entry.getValue().lastSeen > timeout) {
                it.remove();
                if (listener != null) {
                    listener.onServerLost(entry.getKey());
                }
            }
        }
    }
    
    // ==================== COMMUN ====================
    
    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        if (discoveryThread != null) {
            discoveryThread.interrupt();
        }
        discoveredServers.clear();
        System.out.println("[LAN] Découverte arrêtée");
    }
    
    public Collection<ServerInfo> getDiscoveredServers() {
        return new ArrayList<>(discoveredServers.values());
    }
    
    public boolean isRunning() {
        return running;
    }
}
