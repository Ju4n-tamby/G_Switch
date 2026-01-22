package ui.panels;

import core.GameConfig;
import core.GameEngine;
import core.GameState;
import graphics.ResourceManager;
import network.LANDiscovery;
import network.NetworkManager;
import ui.components.NeonButton;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;

/**
 * Panel de configuration réseau - Héberger ou rejoindre une partie
 * Utilise le NetworkManager pour la connexion réelle
 */
public class NetworkPanel extends JPanel {

    private GameEngine engine;
    private Timer animationTimer;
    private ResourceManager resources;
    private NetworkManager networkManager;

    // Mode sélectionné
    private enum PanelMode {
        NONE, HOST, JOIN, LOBBY
    }
    private PanelMode currentMode = PanelMode.NONE;

    // Boutons principaux
    private NeonButton hostButton;
    private NeonButton joinButton;
    private NeonButton backButton;
    private NeonButton startButton;
    private NeonButton refreshButton;
    private NeonButton connectButton;
    private NeonButton readyButton;

    // Champs de saisie
    private String playerName = "Joueur";
    private int maxPlayers = 4;

    // Focus sur les champs
    private int focusedField = -1; // 0: Name, 1: MaxPlayers

    // Liste des serveurs découverts
    private List<LANDiscovery.ServerInfo> discoveredServers = new ArrayList<>();
    private int selectedServerIndex = -1;

    // Liste des joueurs dans le lobby
    private List<NetworkManager.PlayerInfo> lobbyPlayers = new ArrayList<>();
    private boolean isReady = false;

    // Messages de chat/statut
    private List<String> statusMessages = new ArrayList<>();
    private int currentPing = 0;

    // Animation
    private float glowPhase;
    private float cardAlpha = 0f;

    public NetworkPanel(GameEngine engine) {
        this.engine = engine;
        this.resources = ResourceManager.getInstance();
        this.networkManager = NetworkManager.getInstance();

        setPreferredSize(new Dimension(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT));
        setBackground(GameConfig.DARK_BG);
        setFocusable(true);

        initButtons();
        setupMouseListeners();
        setupKeyListeners();
        setupNetworkListener();

        animationTimer = new Timer(16, e -> {
            updateAnimations();
            repaint();
        });
        animationTimer.start();
    }

    private void initButtons() {
        int centerX = GameConfig.WINDOW_WIDTH / 2;
        int buttonWidth = 220;
        int buttonHeight = 55;

        // Boutons de mode
        hostButton = new NeonButton("⚡ HÉBERGER", centerX - buttonWidth - 30, 280,
                buttonWidth, buttonHeight, GameConfig.NEON_CYAN);
        hostButton.setOnClick(this::startHosting);

        joinButton = new NeonButton("🔗 REJOINDRE", centerX + 30, 280,
                buttonWidth, buttonHeight, GameConfig.NEON_PURPLE);
        joinButton.setOnClick(this::startJoining);

        // Bouton retour
        backButton = new NeonButton("← RETOUR", 50, GameConfig.WINDOW_HEIGHT - 80,
                150, 45, GameConfig.NEON_PINK);
        backButton.setOnClick(this::handleBack);

        // Bouton lancer (hôte)
        startButton = new NeonButton("🚀 LANCER", centerX - 100, 620,
                200, 55, GameConfig.NEON_GREEN);
        startButton.setOnClick(this::startGame);

        // Bouton rafraîchir (join)
        refreshButton = new NeonButton("🔄 ACTUALISER", centerX + 150, 620,
                160, 45, GameConfig.NEON_CYAN);
        refreshButton.setOnClick(this::refreshServers);
        
        // Bouton connecter (join)
        connectButton = new NeonButton("➜ CONNECTER", centerX - 150, 620,
                180, 55, GameConfig.NEON_GREEN);
        connectButton.setOnClick(this::connectToServer);

        // Bouton prêt (client)
        readyButton = new NeonButton("✓ PRÊT", centerX - 100, 620,
                200, 55, GameConfig.NEON_GREEN);
        readyButton.setOnClick(this::toggleReady);
    }

    private void setupNetworkListener() {
        networkManager.setListener(new NetworkManager.NetworkListener() {
            @Override
            public void onModeChanged(NetworkManager.NetworkMode mode) {
                if (mode == NetworkManager.NetworkMode.HOST || 
                    mode == NetworkManager.NetworkMode.CLIENT) {
                    currentMode = PanelMode.LOBBY;
                    cardAlpha = 0f;
                }
            }

            @Override
            public void onPlayerListUpdate(List<NetworkManager.PlayerInfo> players) {
                lobbyPlayers = new ArrayList<>(players);
                repaint();
            }

            @Override
            public void onChatMessage(String playerName, String message, boolean isSystem) {
                String prefix = isSystem ? "» " : "[" + playerName + "] ";
                addStatusMessage(prefix + message);
            }

            @Override
            public void onGameStart() {
                // Passer au jeu
                engine.setGameMode(GameConfig.GameMode.NETWORK);
                engine.setState(GameState.PLAYING);
            }
            
            @Override
            public void onReturnToLobby() {
                // Retour au lobby depuis le jeu
                currentMode = PanelMode.LOBBY;
                cardAlpha = 0f;
                addStatusMessage("Retour au lobby");
                engine.setState(GameState.NETWORK);
            }

            @Override
            public void onDisconnected(String reason) {
                addStatusMessage("Déconnecté: " + reason);
                currentMode = PanelMode.NONE;
                networkManager.stopNetwork();
            }

            @Override
            public void onError(String error) {
                addStatusMessage("Erreur: " + error);
                JOptionPane.showMessageDialog(NetworkPanel.this, error, "Erreur", JOptionPane.ERROR_MESSAGE);
            }

            @Override
            public void onPingUpdate(int ping) {
                currentPing = ping;
            }

            @Override
            public void onServerFound(LANDiscovery.ServerInfo server) {
                // Vérifier si le serveur existe déjà
                boolean exists = false;
                for (int i = 0; i < discoveredServers.size(); i++) {
                    if (discoveredServers.get(i).address.equals(server.address)) {
                        discoveredServers.set(i, server);
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    discoveredServers.add(server);
                    addStatusMessage("Serveur trouvé: " + server.serverName);
                }
                repaint();
            }

            @Override
            public void onServerLost(String address) {
                discoveredServers.removeIf(s -> (s.address + ":" + s.tcpPort).equals(address));
                repaint();
            }
        });
    }

    private void addStatusMessage(String message) {
        statusMessages.add(message);
        if (statusMessages.size() > 5) {
            statusMessages.remove(0);
        }
        repaint();
    }

    // === ACTIONS ===

    private void startHosting() {
        if (playerName.trim().isEmpty()) {
            playerName = "Hôte";
        }
        currentMode = PanelMode.HOST;
        cardAlpha = 0f;
        statusMessages.clear();
        addStatusMessage("Préparation du serveur...");

        // Démarrer le serveur
        if (networkManager.hostGame(playerName)) {
            currentMode = PanelMode.LOBBY;
            addStatusMessage("Serveur démarré! En attente de joueurs...");
        }
    }

    private void startJoining() {
        currentMode = PanelMode.JOIN;
        cardAlpha = 0f;
        statusMessages.clear();
        discoveredServers.clear();
        selectedServerIndex = -1;

        addStatusMessage("Recherche de serveurs sur le réseau local...");
        networkManager.startServerSearch();
    }

    private void refreshServers() {
        discoveredServers.clear();
        selectedServerIndex = -1;
        addStatusMessage("Actualisation...");
        networkManager.stopServerSearch();
        networkManager.startServerSearch();
    }

    private void connectToServer() {
        if (selectedServerIndex >= 0 && selectedServerIndex < discoveredServers.size()) {
            LANDiscovery.ServerInfo server = discoveredServers.get(selectedServerIndex);
            if (playerName.trim().isEmpty()) {
                playerName = "Joueur";
            }
            addStatusMessage("Connexion à " + server.serverName + "...");
            networkManager.joinGame(playerName, server.address, server.tcpPort);
        }
    }

    private void toggleReady() {
        isReady = !isReady;
        networkManager.setReady(isReady);
        // Note: NeonButton n'a pas de setText, on gère via l'état
        addStatusMessage(isReady ? "Vous êtes prêt!" : "En attente...");
    }

    private void startGame() {
        if (networkManager.isHost()) {
            networkManager.startGame();
        }
    }

    private void handleBack() {
        if (currentMode == PanelMode.LOBBY) {
            networkManager.stopNetwork();
            currentMode = PanelMode.NONE;
        } else if (currentMode == PanelMode.JOIN) {
            networkManager.stopServerSearch();
            currentMode = PanelMode.NONE;
        } else if (currentMode == PanelMode.HOST) {
            currentMode = PanelMode.NONE;
        } else {
            engine.setState(GameState.MENU);
        }
        statusMessages.clear();
    }

    private void setupMouseListeners() {
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateButtonHovers(e);
            }
        });

        addMouseListener(new MouseAdapter() {
            private boolean clickedOnServer = false;

            @Override
            public void mousePressed(MouseEvent e) {
                clickedOnServer = false;
                checkFieldFocus(e);

                // Gestion de la sélection serveur (priorité haute)
                if (currentMode == PanelMode.JOIN) {
                    int listX = GameConfig.WINDOW_WIDTH / 2 - 200;
                    int listY = 400;
                    int itemHeight = 50;
                    for (int i = 0; i < discoveredServers.size(); i++) {
                        int itemY = listY + i * itemHeight;
                        if (e.getX() >= listX && e.getX() <= listX + 400 &&
                            e.getY() >= itemY && e.getY() <= itemY + itemHeight) {
                            clickedOnServer = true;
                            selectedServerIndex = i;
                            break;
                        }
                    }
                }
                
                // Gestion des boutons (seulement si pas cliqué sur un serveur)
                if (!clickedOnServer) {
                    checkButtonPress(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!clickedOnServer) {
                    checkButtonRelease(e);
                }
                clickedOnServer = false;
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                // Gestion des double-clics serveur
                if (e.getClickCount() == 2 && currentMode == PanelMode.JOIN) {
                    if (selectedServerIndex >= 0 && selectedServerIndex < discoveredServers.size()) {
                        connectToServer();
                    }
                }
            }
        });
    }

    private void updateButtonHovers(MouseEvent e) {
        if (currentMode == PanelMode.NONE) {
            hostButton.setHovered(hostButton.contains(e.getX(), e.getY()));
            joinButton.setHovered(joinButton.contains(e.getX(), e.getY()));
        }
        backButton.setHovered(backButton.contains(e.getX(), e.getY()));

        if (currentMode == PanelMode.LOBBY && networkManager.isHost()) {
            startButton.setHovered(startButton.contains(e.getX(), e.getY()));
        }
        if (currentMode == PanelMode.LOBBY && networkManager.isClient()) {
            readyButton.setHovered(readyButton.contains(e.getX(), e.getY()));
        }
        if (currentMode == PanelMode.JOIN) {
            refreshButton.setHovered(refreshButton.contains(e.getX(), e.getY()));
            connectButton.setHovered(connectButton.contains(e.getX(), e.getY()));
        }
    }

    private void checkButtonPress(MouseEvent e) {
        if (currentMode == PanelMode.NONE) {
            if (hostButton.contains(e.getX(), e.getY())) hostButton.setPressed(true);
            if (joinButton.contains(e.getX(), e.getY())) joinButton.setPressed(true);
        }
        if (backButton.contains(e.getX(), e.getY())) backButton.setPressed(true);

        if (currentMode == PanelMode.LOBBY && networkManager.isHost()) {
            if (startButton.contains(e.getX(), e.getY())) startButton.setPressed(true);
        }
        if (currentMode == PanelMode.LOBBY && networkManager.isClient()) {
            if (readyButton.contains(e.getX(), e.getY())) readyButton.setPressed(true);
        }
        if (currentMode == PanelMode.JOIN) {
            if (refreshButton.contains(e.getX(), e.getY())) refreshButton.setPressed(true);
            if (connectButton.contains(e.getX(), e.getY())) connectButton.setPressed(true);
        }
    }

    private void checkButtonRelease(MouseEvent e) {
        // Ne déclenche que les boutons pertinents au mode courant
        if (currentMode == PanelMode.NONE) {
            if (hostButton.isHovered()) hostButton.click();
            if (joinButton.isHovered()) joinButton.click();
        }

        if (backButton.isHovered()) backButton.click();

        if (currentMode == PanelMode.LOBBY && networkManager.isHost()) {
            if (startButton.isHovered()) startButton.click();
        }

        if (currentMode == PanelMode.LOBBY && networkManager.isClient()) {
            if (readyButton.isHovered()) readyButton.click();
        }

        if (currentMode == PanelMode.JOIN) {
            if (refreshButton.isHovered()) refreshButton.click();
            if (connectButton.isHovered()) connectButton.click();
        }

        hostButton.setPressed(false);
        joinButton.setPressed(false);
        backButton.setPressed(false);
        startButton.setPressed(false);
        readyButton.setPressed(false);
        refreshButton.setPressed(false);
        connectButton.setPressed(false);
    }

    private void checkFieldFocus(MouseEvent e) {
        int cardX = GameConfig.WINDOW_WIDTH / 2 - 200;
        int fieldY = 400;
        int fieldHeight = 45;

        if (e.getX() >= cardX + 20 && e.getX() <= cardX + 380) {
            if (e.getY() >= fieldY && e.getY() <= fieldY + fieldHeight) {
                focusedField = 0; // Nom
            } else {
                focusedField = -1;
            }
        }
        requestFocusInWindow();
    }

    private void setupKeyListeners() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    handleBack();
                    return;
                }

                if (focusedField == 0) { // Nom du joueur
                    if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE && playerName.length() > 0) {
                        playerName = playerName.substring(0, playerName.length() - 1);
                    } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        if (currentMode == PanelMode.JOIN && selectedServerIndex >= 0) {
                            connectToServer();
                        }
                    } else if (Character.isLetterOrDigit(e.getKeyChar()) && playerName.length() < 15) {
                        playerName += e.getKeyChar();
                    }
                }

                if (currentMode == PanelMode.JOIN && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    connectToServer();
                }
            }
        });
    }

    private void updateAnimations() {
        glowPhase += 0.05f;
        if (currentMode != PanelMode.NONE && cardAlpha < 1f) {
            cardAlpha = Math.min(1f, cardAlpha + 0.08f);
        }

        // Mise à jour des boutons
        hostButton.update();
        joinButton.update();
        backButton.update();
        startButton.update();
        refreshButton.update();
        connectButton.update();
        readyButton.update();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Fond
        drawBackground(g2d);

        // Titre
        drawTitle(g2d);

        // Contenu selon le mode
        switch (currentMode) {
            case NONE:
                drawModeSelection(g2d);
                break;
            case HOST:
            case JOIN:
                drawConfigCard(g2d);
                break;
            case LOBBY:
                drawLobby(g2d);
                break;
        }

        // Bouton retour
        backButton.render(g2d);

        // Messages de statut
        drawStatusMessages(g2d);

        g2d.dispose();
    }

    private void drawBackground(Graphics2D g2d) {
        // Fond avec image ou dégradé
        Image bg = resources.getNeonGrid();
        if (bg != null) {
            g2d.drawImage(bg, 0, 0, getWidth(), getHeight(), null);
            g2d.setColor(new Color(0, 0, 20, 200));
            g2d.fillRect(0, 0, getWidth(), getHeight());
        } else {
            GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(10, 10, 30),
                    0, getHeight(), new Color(20, 5, 40)
            );
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }

        // Lignes de grille animées
        g2d.setColor(new Color(GameConfig.NEON_CYAN.getRed(), GameConfig.NEON_CYAN.getGreen(),
                GameConfig.NEON_CYAN.getBlue(), 20));
        g2d.setStroke(new BasicStroke(1));
        for (int i = 0; i < getWidth(); i += 50) {
            int offset = (int) (Math.sin(glowPhase + i * 0.01) * 5);
            g2d.drawLine(i, 0, i + offset, getHeight());
        }
    }

    private void drawTitle(Graphics2D g2d) {
        String title = "MODE RÉSEAU";
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        FontMetrics fm = g2d.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(title)) / 2;
        int y = 120;

        // Glow
        for (int i = 15; i > 0; i--) {
            float alpha = (15 - i) / 15f * 0.3f;
            g2d.setColor(new Color(GameConfig.NEON_PURPLE.getRed(), GameConfig.NEON_PURPLE.getGreen(),
                    GameConfig.NEON_PURPLE.getBlue(), (int) (alpha * 255)));
            g2d.drawString(title, x, y);
        }

        g2d.setColor(Color.WHITE);
        g2d.drawString(title, x, y);

        // Sous-titre selon le mode
        String subtitle;
        switch (currentMode) {
            case HOST: subtitle = "Configuration de l'hébergement"; break;
            case JOIN: subtitle = "Recherche de parties"; break;
            case LOBBY: subtitle = networkManager.isHost() ? "Lobby - Vous êtes l'hôte" : "Lobby - En attente"; break;
            default: subtitle = "Choisissez votre mode de connexion";
        }

        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        fm = g2d.getFontMetrics();
        x = (getWidth() - fm.stringWidth(subtitle)) / 2;
        g2d.setColor(new Color(150, 150, 180));
        g2d.drawString(subtitle, x, 160);

        // Ping si connecté
        if (currentMode == PanelMode.LOBBY && networkManager.isClient()) {
            g2d.setFont(new Font("Consolas", Font.BOLD, 14));
            Color pingColor = currentPing < 50 ? GameConfig.NEON_GREEN :
                    currentPing < 100 ? GameConfig.NEON_CYAN :
                            currentPing < 200 ? new Color(255, 200, 0) : GameConfig.NEON_PINK;
            g2d.setColor(pingColor);
            g2d.drawString("PING: " + currentPing + " ms", getWidth() - 120, 30);
        }
    }

    private void drawModeSelection(Graphics2D g2d) {
        hostButton.render(g2d);
        joinButton.render(g2d);

        // Descriptions
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.setColor(new Color(120, 120, 150));

        int centerX = GameConfig.WINDOW_WIDTH / 2;
        g2d.drawString("Créer une partie sur ce PC", centerX - 220 - 50, 355);
        g2d.drawString("Rejoindre une partie LAN", centerX + 30 + 30, 355);
    }

    private void drawConfigCard(Graphics2D g2d) {
        int cardX = getWidth() / 2 - 220;
        int cardY = 240;
        int cardWidth = 440;
        int cardHeight = currentMode == PanelMode.JOIN ? 450 : 250;

        // Carte avec transparence animée
        Composite oldComp = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, cardAlpha));

        // Fond de la carte
        g2d.setColor(new Color(20, 20, 40, 230));
        g2d.fill(new RoundRectangle2D.Float(cardX, cardY, cardWidth, cardHeight, 20, 20));

        // Bordure
        Color borderColor = currentMode == PanelMode.HOST ? GameConfig.NEON_CYAN : GameConfig.NEON_PURPLE;
        g2d.setColor(borderColor);
        g2d.setStroke(new BasicStroke(2));
        g2d.draw(new RoundRectangle2D.Float(cardX, cardY, cardWidth, cardHeight, 20, 20));

        // Champ nom
        drawInputField(g2d, "Votre pseudo:", playerName, cardX + 20, cardY + 40, 400, focusedField == 0);

        if (currentMode == PanelMode.JOIN) {
            // Liste des serveurs
            drawServerList(g2d, cardX + 20, cardY + 120, 400);
            refreshButton.render(g2d);
            connectButton.render(g2d);
        }

        g2d.setComposite(oldComp);
    }

    private void drawInputField(Graphics2D g2d, String label, String value, int x, int y, int width, boolean focused) {
        // Label
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.setColor(new Color(180, 180, 200));
        g2d.drawString(label, x, y);

        // Champ
        int fieldY = y + 10;
        int fieldHeight = 40;

        g2d.setColor(focused ? new Color(40, 40, 70) : new Color(30, 30, 50));
        g2d.fillRoundRect(x, fieldY, width, fieldHeight, 10, 10);

        Color borderColor = focused ? GameConfig.NEON_CYAN : new Color(80, 80, 120);
        g2d.setColor(borderColor);
        g2d.setStroke(new BasicStroke(focused ? 2 : 1));
        g2d.drawRoundRect(x, fieldY, width, fieldHeight, 10, 10);

        // Valeur
        g2d.setFont(new Font("Consolas", Font.PLAIN, 16));
        g2d.setColor(Color.WHITE);
        g2d.drawString(value + (focused ? "_" : ""), x + 15, fieldY + 26);
    }

    private void drawServerList(Graphics2D g2d, int x, int y, int width) {
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.setColor(new Color(180, 180, 200));
        g2d.drawString("Serveurs disponibles:", x, y);

        int listY = y + 20;
        int itemHeight = 50;

        if (discoveredServers.isEmpty()) {
            g2d.setFont(new Font("Arial", Font.ITALIC, 14));
            g2d.setColor(new Color(100, 100, 130));
            g2d.drawString("Recherche en cours...", x + 20, listY + 30);

            // Animation de chargement
            int dotCount = (int) (glowPhase * 2) % 4;
            StringBuilder dots = new StringBuilder();
            for (int i = 0; i < dotCount; i++) dots.append(".");
            g2d.drawString(dots.toString(), x + 160, listY + 30);
        } else {
            for (int i = 0; i < discoveredServers.size() && i < 5; i++) {
                LANDiscovery.ServerInfo server = discoveredServers.get(i);
                int itemY = listY + i * itemHeight;

                // Fond de l'item
                boolean selected = (i == selectedServerIndex);
                g2d.setColor(selected ? new Color(60, 40, 80) : new Color(30, 30, 50));
                g2d.fillRoundRect(x, itemY, width, itemHeight - 5, 8, 8);

                // Bordure si sélectionné
                if (selected) {
                    g2d.setColor(GameConfig.NEON_PURPLE);
                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawRoundRect(x, itemY, width, itemHeight - 5, 8, 8);
                }

                // Nom du serveur
                g2d.setFont(new Font("Arial", Font.BOLD, 14));
                g2d.setColor(Color.WHITE);
                g2d.drawString(server.serverName, x + 15, itemY + 20);

                // Détails
                g2d.setFont(new Font("Arial", Font.PLAIN, 12));
                g2d.setColor(new Color(150, 150, 180));
                String details = server.playerCount + "/" + server.maxPlayers + " joueurs • " + server.address;
                g2d.drawString(details, x + 15, itemY + 38);

                // Indicateur de statut
                Color statusColor = server.inGame ? new Color(255, 150, 0) : GameConfig.NEON_GREEN;
                g2d.setColor(statusColor);
                g2d.fillOval(x + width - 25, itemY + 18, 10, 10);
            }
        }

        // Instruction
        g2d.setFont(new Font("Arial", Font.ITALIC, 12));
        g2d.setColor(new Color(100, 100, 130));
        g2d.drawString("Double-cliquez pour rejoindre", x, listY + 280);
    }

    private void drawLobby(Graphics2D g2d) {
        int cardX = getWidth() / 2 - 250;
        int cardY = 200;
        int cardWidth = 500;
        int cardHeight = 400;

        // Fond de la carte
        g2d.setColor(new Color(20, 20, 40, 230));
        g2d.fill(new RoundRectangle2D.Float(cardX, cardY, cardWidth, cardHeight, 20, 20));

        // Bordure
        g2d.setColor(GameConfig.NEON_CYAN);
        g2d.setStroke(new BasicStroke(2));
        g2d.draw(new RoundRectangle2D.Float(cardX, cardY, cardWidth, cardHeight, 20, 20));

        // Titre du lobby
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.setColor(Color.WHITE);
        g2d.drawString("LOBBY", cardX + 20, cardY + 40);

        // Nombre de joueurs
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.setColor(new Color(150, 150, 180));
        g2d.drawString(lobbyPlayers.size() + "/" + maxPlayers + " joueurs", cardX + cardWidth - 100, cardY + 35);

        // Liste des joueurs
        int playerY = cardY + 70;
        for (int i = 0; i < lobbyPlayers.size(); i++) {
            NetworkManager.PlayerInfo player = lobbyPlayers.get(i);

            // Fond du joueur
            Color bgColor = player.isHost ? new Color(40, 60, 80) : new Color(30, 30, 50);
            g2d.setColor(bgColor);
            g2d.fillRoundRect(cardX + 20, playerY, cardWidth - 40, 50, 10, 10);

            // Couleur du joueur (indicateur)
            try {
                g2d.setColor(Color.decode(player.colorHex));
            } catch (Exception e) {
                g2d.setColor(GameConfig.NEON_CYAN);
            }
            g2d.fillOval(cardX + 35, playerY + 15, 20, 20);

            // Nom
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.setColor(Color.WHITE);
            g2d.drawString(player.name, cardX + 70, playerY + 30);

            // Badge hôte
            if (player.isHost) {
                g2d.setFont(new Font("Arial", Font.BOLD, 10));
                g2d.setColor(GameConfig.NEON_CYAN);
                g2d.drawString("HÔTE", cardX + 70 + g2d.getFontMetrics(new Font("Arial", Font.BOLD, 16)).stringWidth(player.name) + 10, playerY + 30);
            }

            // Indicateur prêt
            if (player.ready || player.isHost) {
                g2d.setColor(GameConfig.NEON_GREEN);
                g2d.setFont(new Font("Arial", Font.BOLD, 14));
                g2d.drawString("✓ PRÊT", cardX + cardWidth - 100, playerY + 32);
            } else {
                g2d.setColor(new Color(150, 150, 150));
                g2d.setFont(new Font("Arial", Font.PLAIN, 14));
                g2d.drawString("En attente", cardX + cardWidth - 100, playerY + 32);
            }

            playerY += 60;
        }

        // Boutons
        if (networkManager.isHost()) {
            startButton.render(g2d);
        } else {
            readyButton.render(g2d);
        }
    }

    private void drawStatusMessages(Graphics2D g2d) {
        g2d.setFont(new Font("Consolas", Font.PLAIN, 12));
        int y = getHeight() - 120;

        for (int i = 0; i < statusMessages.size(); i++) {
            float alpha = 0.5f + (i / (float) statusMessages.size()) * 0.5f;
            g2d.setColor(new Color(150, 150, 180, (int) (alpha * 255)));
            g2d.drawString("» " + statusMessages.get(i), 60, y + i * 18);
        }
    }

    public void onPanelShown() {
        requestFocusInWindow();
        currentMode = PanelMode.NONE;
        statusMessages.clear();
        discoveredServers.clear();
        lobbyPlayers.clear();
        isReady = false;
    }
}
