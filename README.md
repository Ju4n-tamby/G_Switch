# 🎮 VOID RUNNER - Cyberpunk Edition

## 📖 Description
**VOID RUNNER** est un jeu de plateforme inspiré de G-Switch où vous devez survivre en inversant la gravité pour éviter les obstacles et les trous. Jouez en solo, en local avec vos amis, ou en réseau sur votre réseau local !

> Développé par **Juan, Harry, Aro, Sedra et Mahery** - Projet Réseau 2026

---

## 🚀 Installation et Lancement

### Prérequis
- **Java JDK 21** ou supérieur
- Le JDK doit être dans le PATH système

### Linux / macOS
```bash
# Rendre le script exécutable
chmod +x run.sh

# Lancer le jeu
./run.sh
```

### Windows
```batch
# Double-cliquez sur run.bat
# OU dans le terminal :
run.bat
```

### Compilation manuelle
```bash
# Compiler
javac -encoding UTF-8 -d bin -sourcepath src src/Main.java src/network/*.java

# Exécuter
java -cp bin Main
```

---

## 🎮 Comment Jouer

### Objectif
Survivez le plus longtemps possible en évitant les obstacles et les trous. Atteignez un score de **5 points** pour faire apparaître la **ligne d'arrivée** et remporter la partie !

### Contrôles

| Touche | Action |
|--------|--------|
| **ESPACE** ou **CLIC GAUCHE** | Inverser la gravité |
| **ECHAP** | Pause / Menu |
| **R** | Rejouer (Game Over) |

---

## 🕹️ Modes de Jeu

### 🎯 Mode Solo
Jouez seul et battez votre meilleur score !

### 👥 Mode Local (2-5 joueurs)
Jouez jusqu'à 5 joueurs sur le même clavier. Chaque joueur configure sa touche de saut dans la sélection des joueurs.

### 🌐 Mode Réseau (LAN)
Jouez en multijoueur sur votre réseau local !

---

## 🌐 Guide du Mode Réseau

### Comment héberger une partie

1. Depuis le menu principal, cliquez sur **"RÉSEAU"**
2. Cliquez sur **"⚡ HÉBERGER"**
3. Entrez votre nom de joueur
4. **Le lobby s'affiche** avec la liste des joueurs connectés
5. Attendez que vos amis rejoignent
6. Cliquez sur **"🚀 LANCER"** quand tout le monde est prêt

### Comment rejoindre une partie

1. Depuis le menu principal, cliquez sur **"RÉSEAU"**
2. Cliquez sur **"🔗 REJOINDRE"**
3. Entrez votre nom de joueur
4. **Les serveurs LAN** sont automatiquement détectés
5. Cliquez sur un serveur dans la liste
6. Attendez dans le lobby que l'hôte lance la partie

### Fonctionnalités du lobby

| Élément | Description |
|---------|-------------|
| **Liste des joueurs** | Affiche tous les joueurs connectés avec leur couleur |
| **Bouton "PRÊT"** | Indique que vous êtes prêt à jouer (clients) |
| **Bouton "LANCER"** | Lance la partie (hôte uniquement) |
| **Messages système** | Affiche les connexions/déconnexions |

### Contrôles en pause (Mode Réseau)

| Bouton | Action | Disponible pour |
|--------|--------|-----------------|
| **REPRENDRE** | Reprendre la partie | Tous |
| **RECOMMENCER** | Relancer la partie | Hôte |
| **MENU PRINCIPAL** | Quitter vers le menu | Tous |
| **RETOUR LOBBY** | Renvoie tous les joueurs au lobby | Hôte uniquement |
| **TERMINER PARTIE** | Arrête le serveur et termine la partie | Hôte uniquement |

### Notes importantes

- ⚠️ **Une fois la partie lancée, plus personne ne peut rejoindre**
- 🔄 L'hôte peut **renvoyer tout le monde au lobby** à tout moment
- 📡 Les serveurs sont découverts automatiquement sur le réseau local
- 🏆 Le **vainqueur** est affiché sur l'écran de victoire
- 💀 Si **tous les clients quittent**, l'hôte en est informé

---

## 📡 Architecture Réseau

### Ports utilisés

| Port | Protocole | Usage |
|------|-----------|-------|
| **25565** | TCP | Connexions, chat, commandes |
| **25566** | UDP | Inputs joueurs, état du jeu |
| **25567** | UDP | Découverte automatique LAN |

### Modèle serveur autoritaire

```
┌─────────────────┐           ┌─────────────────┐
│     CLIENT      │           │     SERVEUR     │
│                 │   INPUT   │                 │
│  ┌───────────┐  │ ────────► │  ┌───────────┐  │
│  │ Affichage │  │   (UDP)   │  │  Logique  │  │
│  └───────────┘  │           │  │   de jeu  │  │
│                 │  STATE    │  └───────────┘  │
│                 │ ◄──────── │                 │
│                 │   (UDP)   │                 │
└─────────────────┘           └─────────────────┘
```

- Le **serveur** calcule tout l'état du jeu
- Les **clients** envoient seulement leurs inputs
- L'état du jeu est diffusé à tous les clients

---

## 📁 Structure du Projet

```
Gswitch/
├── run.sh                    # Script de lancement (Linux/Mac)
├── run.bat                   # Script de lancement (Windows)
├── README.md                 # Ce fichier
├── NETWORK_README.md         # Documentation technique réseau
├── src/
│   ├── Main.java             # Point d'entrée
│   ├── core/                 # Moteur de jeu
│   │   ├── GameEngine.java
│   │   ├── GameState.java
│   │   ├── GameConfig.java
│   │   └── PlayerConfig.java
│   ├── entity/               # Entités du jeu
│   │   ├── Player.java
│   │   ├── Obstacle.java
│   │   ├── Hole.java
│   │   └── Gravity.java
│   ├── factory/              # Création d'entités
│   │   ├── ObstacleFactory.java
│   │   └── HoleFactory.java
│   ├── graphics/             # Rendu graphique
│   │   ├── CyberpunkBackground.java
│   │   ├── PlatformRenderer.java
│   │   └── ParticleSystem.java
│   ├── input/                # Gestion des entrées
│   │   └── InputHandler.java
│   ├── network/              # Système réseau
│   │   ├── NetworkManager.java   # Gestionnaire principal
│   │   ├── GameServer.java       # Serveur autoritaire
│   │   ├── GameClient.java       # Client réseau
│   │   ├── LANDiscovery.java     # Découverte automatique
│   │   ├── NetworkProtocol.java  # Protocole et constantes
│   │   └── JsonUtils.java        # Sérialisation JSON
│   └── ui/                   # Interface utilisateur
│       ├── GameWindow.java
│       ├── components/
│       │   ├── NeonButton.java
│       │   └── ChatPanel.java
│       └── panels/
│           ├── MenuPanel.java
│           ├── GamePanel.java
│           ├── NetworkPanel.java
│           ├── InfoPanel.java
│           └── PlayerSelectionPanel.java
├── bin/                      # Fichiers compilés
└── resources/                # Ressources
    ├── images/
    ├── sprites/
    └── sounds/
```

---

## 🎨 Personnalisation

### Ajouter un sprite joueur
1. Placez vos images dans `resources/sprites/`
2. Format : PNG avec transparence
3. Taille recommandée : 40x50 pixels
4. Nommez-les `player.png` et `player_flipped.png`

### Ajouter un fond personnalisé
1. Placez votre image dans `resources/images/`
2. Format : PNG ou JPG
3. Taille recommandée : 1280x720 pixels

---

## 🎨 Style Cyberpunk

Le jeu utilise une palette néon distinctive :

| Couleur | Hex | Utilisation |
|---------|-----|-------------|
| **Cyan** | `#00FFFF` | Joueur, éléments positifs |
| **Pink** | `#FF0080` | Accents, alertes |
| **Purple** | `#B400FF` | Effets secondaires |
| **Green** | `#00FF64` | Succès, victoire |
| **Orange** | `#FF8C00` | Actions, boutons |

---

## 🔧 Configuration

Modifiez `src/core/GameConfig.java` pour ajuster :
- Taille de la fenêtre
- Vitesse du jeu
- Intervalle de spawn des obstacles
- Score pour gagner
- Couleurs du thème
- Ports réseau

---

## 📝 Crédits

**Développeurs :**
- 👨‍💻 Juan
- 👨‍💻 Harry
- 👨‍💻 Aro
- 👨‍💻 Sedra
- 👨‍💻 Mahery

**Inspiré de :** G-Switch

**Version :** 1.0 - Projet Réseau 2026

---

## 📄 Licence

© 2026 - Projet Réseau - Tous droits réservés
