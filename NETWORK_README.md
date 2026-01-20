# VOID RUNNER - Architecture Réseau

## Vue d'ensemble

Ce document décrit l'architecture réseau du jeu VOID RUNNER, conçue pour un projet universitaire de niveau Licence. L'implémentation est simple, lisible et n'utilise aucune librairie externe pour le protocole JSON.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         SERVEUR (Hôte)                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │ TCP Server  │  │ UDP Socket  │  │    LANDiscovery         │  │
│  │ (Connexion) │  │ (Gameplay)  │  │    (Broadcast)          │  │
│  └──────┬──────┘  └──────┬──────┘  └───────────┬─────────────┘  │
│         │                │                     │                │
│         └────────┬───────┴─────────────────────┘                │
│                  │                                              │
│         ┌────────▼────────┐                                     │
│         │   GameServer    │  ← État autoritaire du jeu          │
│         └─────────────────┘                                     │
└─────────────────────────────────────────────────────────────────┘
                              │
                    ┌─────────┴─────────┐
                    │   Réseau Local    │
                    └─────────┬─────────┘
                              │
┌─────────────────────────────┴───────────────────────────────────┐
│                         CLIENT                                   │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │ TCP Socket  │  │ UDP Socket  │  │    LANDiscovery         │  │
│  │ (Connexion) │  │ (Gameplay)  │  │    (Écoute)             │  │
│  └──────┬──────┘  └──────┬──────┘  └───────────┬─────────────┘  │
│         │                │                     │                │
│         └────────┬───────┴─────────────────────┘                │
│                  │                                              │
│         ┌────────▼────────┐                                     │
│         │   GameClient    │  ← Interpolation + prédiction       │
│         └─────────────────┘                                     │
└─────────────────────────────────────────────────────────────────┘
```

## Protocoles utilisés

### TCP (Transmission Control Protocol)
- **Port:** 25565
- **Usage:** Connexion fiable, authentification, chat
- **Caractéristiques:** 
  - Garantie de livraison des messages
  - Ordre des messages préservé
  - Utilisé pour les données critiques (connexion, déconnexion, chat)

### UDP (User Datagram Protocol)
- **Port gameplay:** 25566
- **Port découverte:** 25567
- **Usage:** Inputs joueurs, état du jeu, découverte LAN
- **Caractéristiques:**
  - Pas de garantie de livraison (acceptable pour le temps réel)
  - Faible latence
  - Utilisé pour les données fréquentes (60 fois/seconde)

## Découverte LAN automatique

```
SERVEUR                                    CLIENTS
   │                                          │
   │──── Broadcast UDP (255.255.255.255) ────►│
   │     "VOIDRUNNER_LAN_V1"                  │
   │     + nom serveur                        │
   │     + nombre joueurs                     │
   │     + port TCP                           │
   │                                          │
   │◄─────────── Connexion TCP ───────────────│
   │                                          │
```

Le serveur diffuse sa présence toutes les 2 secondes sur le port 25567. Les clients en recherche écoutent ce port et maintiennent une liste des serveurs disponibles.

## Format des messages JSON

### Implémentation JSON maison

Pour éviter les dépendances externes (conformément aux exigences du projet), une classe `JsonUtils` a été implémentée :

```java
// Sérialisation
String json = JsonUtils.builder()
    .put("type", "CHAT_MESSAGE")
    .put("playerId", 1)
    .put("message", "Salut!")
    .build();

// Désérialisation
Map<String, Object> data = JsonUtils.parse(json);
String type = JsonUtils.getString(data, "type", "");
```

### Messages TCP

#### Connexion
```json
// Demande de connexion (Client → Serveur)
{
  "type": "CONNECT_REQUEST",
  "playerName": "Juan",
  "version": "1.0"
}

// Connexion acceptée (Serveur → Client)
{
  "type": "CONNECT_ACCEPT",
  "playerId": 1,
  "serverName": "Partie de Juan",
  "players": [
    {"id": 0, "name": "Juan", "color": "#00FFFF", "ready": true, "isHost": true}
  ]
}

// Connexion refusée (Serveur → Client)
{
  "type": "CONNECT_REJECT",
  "reason": "Partie pleine"
}
```

#### Chat
```json
{
  "type": "CHAT_MESSAGE",
  "playerId": 1,
  "playerName": "Juan",
  "message": "Hello!",
  "timestamp": 1705600000000
}
```

#### Ping/Pong
```json
// Ping (Client → Serveur)
{"type": "PING", "timestamp": 1705600000000}

// Pong (Serveur → Client)  
{"type": "PONG", "timestamp": 1705600000000}
```

### Messages UDP

#### Input joueur (Client → Serveur)
```json
{
  "type": "INPUT",
  "playerId": 1,
  "sequence": 12345,
  "action": "GRAVITY_SWITCH",
  "timestamp": 1705600000000
}
```

#### État du jeu (Serveur → Clients)
```json
{
  "type": "GAME_STATE",
  "tick": 1234,
  "players": [
    {
      "id": 0,
      "x": 100,
      "y": 300,
      "vy": 5.0,
      "gravity": "DOWN",
      "alive": true,
      "score": 5
    }
  ],
  "holes": [
    {"x": 400, "width": 80}
  ],
  "obstacles": [
    {"x": 600, "y": 350, "width": 40, "height": 100}
  ]
}
```

## Modèle client-serveur autoritaire

### Principe

Le **serveur fait autorité** sur l'état du jeu :
1. Les clients envoient uniquement leurs **inputs** (actions)
2. Le serveur traite les inputs et met à jour l'état du jeu
3. Le serveur diffuse l'état à tous les clients
4. Les clients **affichent** l'état reçu

```
CLIENT A        SERVEUR         CLIENT B
   │               │               │
   │──INPUT(saut)──►               │
   │               │               │
   │               │◄──INPUT(saut)─│
   │               │               │
   │   Calcul physique + collisions│
   │               │               │
   │◄──GAME_STATE──┼──GAME_STATE──►│
   │               │               │
   │   Affichage   │   Affichage   │
```

### Avantages
- **Anti-triche:** Impossible de modifier l'état localement
- **Cohérence:** Tous les joueurs voient le même état
- **Simplicité:** Logique de jeu centralisée

### Inconvénients
- **Latence:** Délai entre input et retour visuel
- **Bande passante:** État complet envoyé fréquemment

## Structure du code

```
src/network/
├── JsonUtils.java        # Sérialisation/désérialisation JSON maison
├── NetworkProtocol.java  # Constantes et définition du protocole
├── LANDiscovery.java     # Découverte automatique des serveurs LAN
├── GameServer.java       # Serveur de jeu autoritaire
├── GameClient.java       # Client de jeu
└── NetworkManager.java   # Facade simplifiant l'utilisation
```

### Diagramme de classes

```
┌─────────────────────┐
│   NetworkManager    │ ← Singleton, point d'entrée principal
├─────────────────────┤
│ - server: GameServer│
│ - client: GameClient│
├─────────────────────┤
│ + hostGame()        │
│ + joinGame()        │
│ + sendChat()        │
│ + sendGravitySwitch │
└─────────────────────┘
         │
    ┌────┴────┐
    ▼         ▼
┌────────┐  ┌────────┐
│GameServ│  │GameClie│
│   er   │  │   nt   │
└────────┘  └────────┘
    │            │
    └─────┬──────┘
          ▼
   ┌──────────────┐
   │ LANDiscovery │
   └──────────────┘
```

## Utilisation

### Héberger une partie

```java
NetworkManager network = NetworkManager.getInstance();
network.setListener(myListener);

// Démarrer le serveur
if (network.hostGame("MonPseudo")) {
    System.out.println("Serveur démarré!");
}

// Démarrer la partie quand tous sont prêts
network.startGame();
```

### Rejoindre une partie

```java
NetworkManager network = NetworkManager.getInstance();
network.setListener(myListener);

// Chercher les serveurs sur le LAN
network.startServerSearch();

// Quand un serveur est trouvé (via callback)
LANDiscovery.ServerInfo server = ...;
network.joinGame("MonPseudo", server.address, server.tcpPort);

// Se déclarer prêt
network.setReady(true);
```

### Envoyer des inputs

```java
// Changement de gravité
network.sendGravitySwitch();

// Message chat
network.sendChat("Bien joué!");
```

## Flux de données pendant une partie

```
     CLIENT                           SERVEUR
        │                                │
        │   ┌─────────────────────┐      │
        │   │ Appui touche saut   │      │
        │   └──────────┬──────────┘      │
        │              │                 │
        │              ▼                 │
        │   ┌─────────────────────┐      │
        │   │ sendGravitySwitch() │      │
        │   └──────────┬──────────┘      │
        │              │                 │
        │──────UDP INPUT──────────────────►
        │                                │
        │              ┌─────────────────┤
        │              │ processInput()  │
        │              ├─────────────────┤
        │              │ updatePhysics() │
        │              └────────┬────────┘
        │                       │
        │◄──────UDP GAME_STATE──┼─────────
        │                       │
        ├───────────────────────┘
        │ updateLocalState()
        │ render()
        ▼
```

## Gestion des erreurs

- **Timeout connexion:** 5 secondes
- **Serveur injoignable:** Message d'erreur à l'utilisateur
- **Déconnexion:** Nettoyage automatique et notification
- **Paquets UDP perdus:** Ignorés (le prochain état écrasera)

## Évolutions possibles

1. **Interpolation client-side:** Lisser les mouvements entre deux états reçus
2. **Prédiction client-side:** Appliquer l'input localement avant confirmation serveur
3. **Compression:** Réduire la taille des messages GAME_STATE
4. **Delta encoding:** Envoyer uniquement les changements
5. **Reconnexion:** Permettre de rejoindre une partie en cours

## Développeurs

- **Juan** - Architecture réseau
- **Harry** - Gameplay
- **Aro** - Interface utilisateur
- **Sedra** - Tests et documentation
- **Mahery** - Graphismes

---

*Projet universitaire - Janvier 2026*
