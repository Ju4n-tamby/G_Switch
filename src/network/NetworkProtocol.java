package network;

/**
 * Définition du protocole de communication réseau
 * 
 * Architecture:
 * - TCP: Connexion initiale, authentification, chat
 * - UDP: Inputs joueurs (client→serveur), état du jeu (serveur→clients)
 * 
 * Format: JSON structuré
 */
public class NetworkProtocol {
    
    // === CONFIGURATION RÉSEAU ===
    
    public static final int TCP_PORT = 25565;           // Port TCP principal
    public static final int UDP_PORT = 25566;           // Port UDP pour le gameplay
    public static final int DISCOVERY_PORT = 25567;     // Port UDP pour découverte LAN
    
    public static final int TICK_RATE = 60;             // Mises à jour par seconde
    public static final int TICK_INTERVAL = 1000 / TICK_RATE;
    
    public static final String DISCOVERY_MAGIC = "VOIDRUNNER_LAN_V1";
    public static final int DISCOVERY_INTERVAL = 2000;  // Broadcast toutes les 2s
    public static final int CONNECTION_TIMEOUT = 5000;  // 5 secondes timeout
    
    public static final int MAX_PLAYERS = 4;
    
    // === TYPES DE MESSAGES ===
    
    /**
     * Messages TCP (connexion fiable)
     */
    public enum TcpMessageType {
        // Connexion
        CONNECT_REQUEST,    // Client → Serveur: demande de connexion
        CONNECT_ACCEPT,     // Serveur → Client: connexion acceptée
        CONNECT_REJECT,     // Serveur → Client: connexion refusée
        DISCONNECT,         // Bidirectionnel: déconnexion propre
        
        // Lobby
        PLAYER_LIST,        // Serveur → Clients: liste des joueurs connectés
        PLAYER_READY,       // Client → Serveur: joueur prêt
        GAME_START,         // Serveur → Clients: lancement de la partie
        
        // Chat
        CHAT_MESSAGE,       // Bidirectionnel: message de chat
        
        // Synchronisation
        PING,               // Bidirectionnel: mesure de latence
        PONG                // Réponse au ping
    }
    
    /**
     * Messages UDP (temps réel, sans garantie)
     */
    public enum UdpMessageType {
        INPUT,              // Client → Serveur: action du joueur
        GAME_STATE,         // Serveur → Clients: état complet du jeu
        PLAYER_STATE        // Serveur → Clients: état d'un joueur (delta)
    }
    
    // === FORMATS DES MESSAGES ===
    
    /*
     * CONNECT_REQUEST:
     * {
     *   "type": "CONNECT_REQUEST",
     *   "playerName": "Juan",
     *   "version": "1.0"
     * }
     * 
     * CONNECT_ACCEPT:
     * {
     *   "type": "CONNECT_ACCEPT",
     *   "playerId": 1,
     *   "serverName": "Partie de Juan",
     *   "players": [
     *     {"id": 0, "name": "Juan", "color": "#00FFFF", "ready": true},
     *     {"id": 1, "name": "Harry", "color": "#FF00FF", "ready": false}
     *   ]
     * }
     * 
     * CONNECT_REJECT:
     * {
     *   "type": "CONNECT_REJECT",
     *   "reason": "Partie pleine"
     * }
     * 
     * PLAYER_LIST:
     * {
     *   "type": "PLAYER_LIST",
     *   "players": [...]
     * }
     * 
     * PLAYER_READY:
     * {
     *   "type": "PLAYER_READY",
     *   "ready": true
     * }
     * 
     * GAME_START:
     * {
     *   "type": "GAME_START",
     *   "seed": 123456789,
     *   "countdown": 3
     * }
     * 
     * CHAT_MESSAGE:
     * {
     *   "type": "CHAT_MESSAGE",
     *   "playerId": 1,
     *   "playerName": "Juan",
     *   "message": "Salut tout le monde!",
     *   "timestamp": 1705600000000
     * }
     * 
     * PING/PONG:
     * {
     *   "type": "PING",
     *   "timestamp": 1705600000000
     * }
     * 
     * INPUT (UDP):
     * {
     *   "type": "INPUT",
     *   "playerId": 1,
     *   "sequence": 12345,
     *   "action": "GRAVITY_SWITCH",
     *   "timestamp": 1705600000000
     * }
     * 
     * GAME_STATE (UDP):
     * {
     *   "type": "GAME_STATE",
     *   "tick": 1234,
     *   "players": [
     *     {"id": 0, "x": 100, "y": 300, "vy": 5.0, "gravity": "DOWN", "alive": true, "score": 5},
     *     {"id": 1, "x": 100, "y": 450, "vy": -3.0, "gravity": "UP", "alive": true, "score": 4}
     *   ],
     *   "holes": [
     *     {"x": 400, "width": 80, "isGround": true}
     *   ],
     *   "obstacles": [
     *     {"x": 600, "y": 350, "width": 40, "height": 100}
     *   ]
     * }
     * 
     * LAN_DISCOVERY (broadcast UDP):
     * {
     *   "magic": "VOIDRUNNER_LAN_V1",
     *   "serverName": "Partie de Juan",
     *   "playerCount": 2,
     *   "maxPlayers": 4,
     *   "tcpPort": 25565,
     *   "inGame": false
     * }
     */
    
    // === ACTIONS DU JOUEUR ===
    
    public enum PlayerAction {
        GRAVITY_SWITCH,     // Changement de gravité
        NONE                // Pas d'action
    }
}
