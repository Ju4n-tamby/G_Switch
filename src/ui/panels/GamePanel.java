package ui.panels;

import core.GameConfig;
import core.GameEngine;
import core.GameState;
import core.PlayerConfig;
import factory.*;
import factory.entity.*;
import graphics.*;
import input.InputHandler;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.*;
import ui.components.ChatPanel;
import ui.components.NeonButton;

/**
 * Panel principal du jeu (support multijoueur local et réseau)
 */
public class GamePanel extends JPanel implements ActionListener, InputHandler.InputCallback {

    private GameEngine engine;
    private Timer gameTimer;
    private InputHandler inputHandler;

    // Mode de jeu
    private GameConfig.GameMode gameMode = GameConfig.GameMode.SOLO;

    // Entités multijoueur
    private List<Player> players;
    private Player player;  // Pour compatibilité rétroactive (premier joueur)
    private List<Hole> holes;
    private List<Obstacle> obstacles;

    // Graphiques
    private CyberpunkBackground background;
    private PlatformRenderer platformRenderer;

    // État du jeu
    private boolean isPaused;
    private boolean isGameOver;
    private boolean isVictory;
    private int frameCount;
    private int obstacleFrameCount;

    // Ligne d'arrivée
    private static final int SCORE_TO_WIN = 5; // Score nécessaire pour déclencher la ligne d'arrivée
    private double finishLineX; // Position de la ligne d'arrivée
    private boolean finishLineActive; // Si la ligne d'arrivée est apparue
    private float victoryAlpha; // Animation de victoire
    private float victoryScale;

    // UI en jeu
    private ChatPanel chatPanel;
    private boolean chatVisible;
    private List<NeonButton> pauseButtons;
    private List<NeonButton> gameOverButtons;

    // Animation Game Over
    private float gameOverAlpha;
    private float gameOverScale;

    // Vainqueur de la partie
    private Player winner;

    public GamePanel(GameEngine engine) {
        this.engine = engine;

        setPreferredSize(new Dimension(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT));
        setBackground(GameConfig.DARK_BG);
        setFocusable(true);
        setLayout(null); // Layout manuel pour le chat

        // Activer le double buffering pour des performances optimales
        setDoubleBuffered(true);

        // Initialisation
        initGame();
        initGraphics();
        initUI();
        setupInput();

        // Timer de jeu avec priorité haute
        gameTimer = new Timer(GameConfig.FRAME_TIME, this);
        gameTimer.setCoalesce(true); // Fusionner les événements en retard
    }

    private void initGame() {
        players = new ArrayList<>();
        player = new Player();  // Créer le premier joueur par défaut
        players.add(player);
        holes = new ArrayList<>();
        obstacles = new ArrayList<>();
        frameCount = 0;
        obstacleFrameCount = 0;
        isPaused = false;
        isGameOver = false;
        gameOverAlpha = 0;
        gameOverScale = 0;
    }

    private void initGraphics() {
        background = new CyberpunkBackground();
        platformRenderer = new PlatformRenderer();
    }

    private void initUI() {
        // Chat panel (pour mode réseau uniquement)
        chatPanel = new ChatPanel();
        chatPanel.setBounds(GameConfig.WINDOW_WIDTH - 320, GameConfig.WINDOW_HEIGHT - 220, 300, 200);
        add(chatPanel);
        chatVisible = false;

        // Boutons de pause
        pauseButtons = new ArrayList<>();
        int buttonWidth = 250;
        int buttonHeight = 50;
        int centerX = GameConfig.WINDOW_WIDTH / 2 - buttonWidth / 2;
        int startY = 320;

        NeonButton resumeBtn = new NeonButton("REPRENDRE", centerX, startY, buttonWidth, buttonHeight, GameConfig.NEON_CYAN);
        resumeBtn.setOnClick(this::resumeGame);

        NeonButton restartBtn = new NeonButton("RECOMMENCER", centerX, startY + 70, buttonWidth, buttonHeight, GameConfig.NEON_PURPLE);
        restartBtn.setOnClick(() -> {
            resetGame();
            resumeGame();
        });

        NeonButton menuBtn = new NeonButton("MENU PRINCIPAL", centerX, startY + 140, buttonWidth, buttonHeight, GameConfig.NEON_PINK);
        menuBtn.setOnClick(() -> engine.setState(GameState.MENU));

        // Bouton retour au lobby (pour l'hébergeur en mode réseau)
        NeonButton lobbyBtn = new NeonButton("RETOUR LOBBY", centerX, startY + 210, buttonWidth, buttonHeight, GameConfig.NEON_ORANGE);
        lobbyBtn.setOnClick(() -> {
            // Renvoyer tous les joueurs au lobby
            if (network.NetworkManager.getInstance().isHost()) {
                network.NetworkManager.getInstance().returnToLobby();
                engine.setState(GameState.NETWORK);
            }
        });

        // Bouton quitter la partie (pour l'hébergeur en mode réseau)
        NeonButton quitGameBtn = new NeonButton("TERMINER PARTIE", centerX, startY + 280, buttonWidth, buttonHeight, new java.awt.Color(255, 60, 60));
        quitGameBtn.setOnClick(() -> {
            // Fermer le serveur si on est l'hébergeur
            network.NetworkManager.getInstance().stopNetwork();
            engine.setState(GameState.MENU);
        });

        pauseButtons.add(resumeBtn);
        pauseButtons.add(restartBtn);
        pauseButtons.add(menuBtn);
        pauseButtons.add(lobbyBtn);
        pauseButtons.add(quitGameBtn);

        // Boutons Game Over
        gameOverButtons = new ArrayList<>();

        NeonButton retryBtn = new NeonButton("REJOUER", centerX, startY + 80, buttonWidth, buttonHeight, GameConfig.NEON_CYAN);
        retryBtn.setOnClick(() -> {
            // En mode réseau, l'hôte redémarre pour tout le monde
            if (gameMode == GameConfig.GameMode.NETWORK && network.NetworkManager.getInstance().isHost()) {
                resetGame();
                // Ramener tout le monde au lobby puis relancer
                network.NetworkManager.getInstance().returnToLobby();
                network.NetworkManager.getInstance().startGame();
            } else {
                resetGame();
                engine.setState(GameState.PLAYING);
            }
        });

        NeonButton goMenuBtn = new NeonButton("MENU PRINCIPAL", centerX, startY + 150, buttonWidth, buttonHeight, GameConfig.NEON_PINK);
        goMenuBtn.setOnClick(() -> engine.setState(GameState.MENU));

        gameOverButtons.add(retryBtn);
        gameOverButtons.add(goMenuBtn);

        setupButtonListeners();
    }

    private void setupButtonListeners() {
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (isPaused) {
                    for (NeonButton btn : pauseButtons) {
                        btn.setHovered(btn.contains(e.getX(), e.getY()));
                    }
                }
                if (isGameOver || isVictory) {
                    for (NeonButton btn : gameOverButtons) {
                        btn.setHovered(btn.contains(e.getX(), e.getY()));
                    }
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Clic gauche pour inverser la gravité (en jeu uniquement)
                if (!isPaused && !isGameOver && !isVictory && e.getButton() == MouseEvent.BUTTON1) {
                    // Vérifier qu'on ne clique pas sur un bouton
                    boolean onButton = false;
                    for (NeonButton btn : pauseButtons) {
                        if (btn.contains(e.getX(), e.getY())) {
                            onButton = true;
                        }
                    }
                    for (NeonButton btn : gameOverButtons) {
                        if (btn.contains(e.getX(), e.getY())) {
                            onButton = true;
                        }
                    }
                    if (!onButton) {
                        // Appeler la logique d'input comme pour la touche
                        if (player != null) {
                            onPlayerGravitySwitch(player.getPlayerId());
                        }
                    }
                }

                if (isPaused) {
                    for (NeonButton btn : pauseButtons) {
                        if (btn.contains(e.getX(), e.getY())) {
                            btn.setPressed(true);
                        }
                    }
                }
                if (isGameOver || isVictory) {
                    for (NeonButton btn : gameOverButtons) {
                        if (btn.contains(e.getX(), e.getY())) {
                            btn.setPressed(true);
                        }
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isPaused) {
                    for (NeonButton btn : pauseButtons) {
                        if (btn.isHovered()) {
                            btn.click();
                        }
                        btn.setPressed(false);
                    }
                }
                if (isGameOver || isVictory) {
                    for (NeonButton btn : gameOverButtons) {
                        if (btn.isHovered()) {
                            btn.click();
                        }
                        btn.setPressed(false);
                    }
                }
            }
        });
    }

    private void setupInput() {
        inputHandler = new InputHandler(engine);
        inputHandler.setCallback(this);
        // Les configurations seront passées par setPlayerConfigs()
        addKeyListener(inputHandler);

        // Plus de touche T pour le chat - le chat s'ouvre automatiquement
        // quand un joueur meurt en mode réseau
    }

    // === GESTION DU JEU ===
    public void startGame() {
        isPaused = false;
        isGameOver = false;
        gameTimer.start();
        requestFocusInWindow();
    }

    public void pauseGame() {
        isPaused = true;
    }

    public void resumeGame() {
        isPaused = false;
        requestFocusInWindow();
    }

    public void gameOver() {
        isGameOver = true;
        gameOverAlpha = 0;
        gameOverScale = 0;
    }

    public void setPlayerConfigs(List<PlayerConfig> configs) {
        // Créer les joueurs à partir des configurations
        players.clear();

        int startX = GameConfig.PLAYER_START_X;
        int yOffset = 0;

        for (PlayerConfig config : configs) {
            Player p = new Player(config.getPlayerId(), config.getPlayerName(), config.getPlayerColor());
            // Décaler légèrement chaque joueur pour qu'ils ne se superposent pas au départ
            p.setY(GameConfig.GROUND_Y - GameConfig.PLAYER_HEIGHT - yOffset);
            players.add(p);
            yOffset += 5; // Petit décalage vertical
        }

        // Le premier joueur est le joueur "principal" pour la compatibilité
        if (!players.isEmpty()) {
            player = players.get(0);
        }

        // En mode réseau client, s'assurer qu'on a au moins la config du joueur local pour l'input
        if (gameMode == GameConfig.GameMode.NETWORK && players.size() > 0 && configs.isEmpty()) {
            // Ajoute une config par défaut pour le joueur 0 (ESPACE)
            List<PlayerConfig> defaultConfig = new java.util.ArrayList<>();
            defaultConfig.add(PlayerConfig.createWithScheme(1));
            inputHandler.setPlayerConfigs(defaultConfig);
        } else {
            inputHandler.setPlayerConfigs(configs);
        }
    }

    /**
     * Charge les joueurs depuis le NetworkManager en mode réseau
     */
    public void loadNetworkPlayers() {
        network.NetworkManager networkManager = network.NetworkManager.getInstance();
        List<entity.Player> networkPlayers = networkManager.getNetworkPlayers();

        if (!networkPlayers.isEmpty()) {
            players.clear();
            // Convertir les joueurs du réseau (entity.Player) en factory.entity.Player
            for (entity.Player p : networkPlayers) {
                factory.entity.Player factoryPlayer = new factory.entity.Player(p.getPlayerId(), p.getPlayerName(), p.getPlayerColor());
                // Copier la position et l'état
                factoryPlayer.setX(p.getX());
                factoryPlayer.setY(p.getY());
                players.add(factoryPlayer);
            }
            player = players.get(0);
            System.out.println("[GamePanel] Joueurs chargés depuis le réseau: " + players.size());
        }
    }

    /**
     * Synchronise entièrement l'état local avec l'état réseau (client).
     */
    private void syncNetworkState(network.NetworkManager networkManager) {
        List<entity.Player> netPlayers = networkManager.getNetworkPlayers();
        if (!netPlayers.isEmpty()) {
            players.clear();
            for (entity.Player p : netPlayers) {
                factory.entity.Player fp = new factory.entity.Player(p.getPlayerId(), p.getPlayerName(), p.getPlayerColor());
                fp.setX(p.getX());
                fp.setY(p.getY());
                fp.setVelocityY(p.getVelocityY());
                fp.setGravity(factory.entity.Gravity.valueOf(p.getGravity().name()));
                if (!p.isAlive()) {
                    fp.die();
                }
                fp.setScore(p.getScore());
                players.add(fp);
            }
            player = players.get(0);
        }

        List<entity.Hole> netHoles = networkManager.getNetworkHoles();
        holes.clear();
        for (entity.Hole h : netHoles) {
            holes.add(new factory.entity.Hole(h.getX(), h.getWidth()));
        }

        List<entity.Obstacle> netObstacles = networkManager.getNetworkObstacles();
        obstacles.clear();
        for (entity.Obstacle o : netObstacles) {
            obstacles.add(new factory.entity.Obstacle((int) o.getX(), (int) o.getY(), o.getWidth(), o.getHeight()));
        }
    }

    public void resetGame() {
        // Réinitialiser tous les joueurs
        for (Player p : players) {
            p.reset();
        }

        holes.clear();
        obstacles.clear();
        frameCount = 0;
        obstacleFrameCount = 0;
        isGameOver = false;
        isVictory = false;
        isPaused = false;
        gameOverAlpha = 0;
        gameOverScale = 0;
        finishLineX = GameConfig.WINDOW_WIDTH + 200;
        finishLineActive = false;
        victoryAlpha = 0;
        victoryScale = 0;
        winner = null;

        // Reset du chat
        chatPanel.reset();

        // Important: remettre le focus pour que les touches fonctionnent
        SwingUtilities.invokeLater(() -> {
            requestFocusInWindow();
        });
    }

    /**
     * Définit le mode de jeu (SOLO, LOCAL, NETWORK)
     */
    public void setGameMode(GameConfig.GameMode mode) {
        this.gameMode = mode;

        // Activer le chat uniquement en mode réseau
        if (mode == GameConfig.GameMode.NETWORK) {
            chatPanel.enableChat(true);
            chatVisible = true;
        } else {
            chatPanel.enableChat(false);
            chatVisible = false;
        }
    }

    /**
     * Vérifie si on est en mode réseau
     */
    public boolean isNetworkMode() {
        return gameMode == GameConfig.GameMode.NETWORK;
    }

    /**
     * Appelé quand un joueur meurt - active le chat pour lui en mode réseau
     */
    private void onPlayerDeath(Player deadPlayer) {
        if (isNetworkMode()) {
            chatPanel.activateInput(deadPlayer.getPlayerName());
        }
    }

    // === CALLBACKS INPUT ===
    @Override
    public void onPlayerGravitySwitch(int playerId) {
        if (isPaused || isGameOver || isVictory) {
            return;
        }

        // Mode réseau : le client envoie l'input au serveur au lieu de simuler localement
        if (gameMode == GameConfig.GameMode.NETWORK) {
            network.NetworkManager nm = network.NetworkManager.getInstance();
            if (nm.isClient()) {
                nm.sendGravitySwitch();
                return;
            }
            // Hôte : on continue à appliquer localement (serveur diffusé ensuite)
        }

        // Local/host: appliquer directement sur le joueur
        for (Player p : players) {
            if (p.getPlayerId() == playerId && p.isAlive()) {
                p.switchGravity();
                break;
            }
        }
    }

    @Override
    public void onPausePressed() {
        if (!isGameOver && !isVictory) {
            if (isPaused) {
                resumeGame();
            } else {
                pauseGame();
            }
        }
    }

    @Override
    public void onRestartPressed() {
        // Non utilisé - le restart se fait via les boutons
    }

    // === BOUCLE DE JEU ===
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isPaused && !isGameOver && !isVictory) {
            update();
        } else if (isGameOver) {
            // Animation game over
            gameOverAlpha = Math.min(1.0f, gameOverAlpha + 0.03f);
            gameOverScale = Math.min(1.0f, gameOverScale + 0.05f);
        } else if (isVictory) {
            // Animation victoire
            victoryAlpha = Math.min(1.0f, victoryAlpha + 0.03f);
            victoryScale = Math.min(1.0f, victoryScale + 0.05f);
        }

        // Mise à jour des boutons
        if (isPaused) {
            for (NeonButton btn : pauseButtons) {
                btn.update();
            }
        }
        if (isGameOver || isVictory) {
            for (NeonButton btn : gameOverButtons) {
                btn.update();
            }
        }

        repaint();
    }

    private void update() {
        // Mode réseau: les clients ne simulent pas, ils consomment l'état du serveur
        if (gameMode == GameConfig.GameMode.NETWORK) {
            network.NetworkManager nm = network.NetworkManager.getInstance();
            // Client: récupérer l'état réseau et ne rien simuler localement
            if (nm.isClient()) {
                background.update();
                platformRenderer.update();
                syncNetworkState(nm);
                // Toujours demander un repaint pour garder le framerate fluide
                repaint();
                return;
            }
            // Hôte: continue la simulation puis synchronise/broadcast l'état plus bas
        }

        // Mise à jour du fond
        background.update();
        platformRenderer.update();

        // Génération des trous
        frameCount++;
        if (frameCount % GameConfig.HOLE_SPAWN_INTERVAL == 0) {
            holes.add(HoleFactory.generate(GameConfig.WINDOW_WIDTH));
        }

        // Génération des obstacles
        obstacleFrameCount++;
        if (obstacleFrameCount % GameConfig.OBSTACLE_SPAWN_INTERVAL == 0) {
            Obstacle newObs = ObstacleFactory.generate(GameConfig.WINDOW_WIDTH);

            // Vérifier la distance avec les trous
            boolean tooClose = false;
            for (Hole h : holes) {
                if (Math.abs(newObs.getX() - h.getX()) < GameConfig.MIN_SPAWN_DISTANCE) {
                    tooClose = true;
                    break;
                }
            }

            if (!tooClose) {
                obstacles.add(newObs);
            }
        }

        // Mise à jour de tous les joueurs
        for (Player p : players) {
            p.update();

            // Vérifier si le joueur est poussé hors de l'écran (à gauche)
            if (p.isAlive() && p.getX() + p.getWidth() < 0) {
                p.die();
                onPlayerDeath(p);
                checkAllPlayersDead();
            }
        }

        // Mise à jour des trous
        Iterator<Hole> holeIt = holes.iterator();
        while (holeIt.hasNext()) {
            Hole h = holeIt.next();
            h.update();
            if (h.isOffScreen()) {
                holeIt.remove();
                // Ajouter le score à tous les joueurs vivants
                for (Player p : players) {
                    if (p.isAlive()) {
                        p.addScore(1);
                    }
                }
            }
        }

        // Mise à jour des obstacles
        Iterator<Obstacle> obsIt = obstacles.iterator();
        while (obsIt.hasNext()) {
            Obstacle o = obsIt.next();
            o.update();
            if (o.isOffScreen()) {
                obsIt.remove();
            }
        }

        // Collision avec obstacles
        checkObstacleCollisions();

        // Collision avec sol/plafond/trous
        checkPlatformCollisions();

        // Gestion de la ligne d'arrivée
        updateFinishLine();

        // Si on est l'hôte réseau, diffuser l'état simulé à chaque tick
        if (gameMode == GameConfig.GameMode.NETWORK) {
            network.NetworkManager.getInstance().syncHostState(players, holes, obstacles);
        }
    }

    private void updateFinishLine() {
        // Activer la ligne d'arrivée quand un joueur atteint le score requis
        int maxScore = 0;
        for (Player p : players) {
            if (p.getScore() > maxScore) {
                maxScore = p.getScore();
            }
        }

        if (!finishLineActive && maxScore >= SCORE_TO_WIN) {
            finishLineActive = true;
            finishLineX = GameConfig.WINDOW_WIDTH + 100;
        }

        // Faire défiler la ligne d'arrivée vers le joueur
        if (finishLineActive) {
            finishLineX -= GameConfig.GAME_SPEED;

            // Vérifier si un joueur a atteint la ligne d'arrivée
            for (Player p : players) {
                if (p.isAlive() && p.getX() + p.getWidth() >= finishLineX - 10) {
                    winner = p; // Enregistrer le vainqueur
                    isVictory = true;
                    victoryAlpha = 0;
                    victoryScale = 0;
                    break;
                }
            }
        }
    }

    private void checkObstacleCollisions() {
        for (Player p : players) {
            if (!p.isAlive()) {
                continue;
            }

            Rectangle playerBounds = p.getBounds();

            for (Obstacle obs : obstacles) {
                Rectangle obsBounds = obs.getBounds();

                if (playerBounds.intersects(obsBounds)) {
                    // Toute collision avec un obstacle est mortelle
                    // Cela évite le bug où le joueur reste piégé sur un obstacle
                    p.die();
                    onPlayerDeath(p);
                    checkAllPlayersDead();
                    break;
                }
            }
        }
    }

    private void checkPlatformCollisions() {
        for (Player p : players) {
            // Vérifier si le joueur est hors de l'écran (Y trop haut ou trop bas) - fonctionne même si mort
            if (p.isFalling()) {
                // Gravité vers le bas: mort si trop bas
                if (p.getGravity() == Gravity.DOWN && p.getY() > GameConfig.WINDOW_HEIGHT) {
                    if (p.isAlive()) {
                        p.die();
                        onPlayerDeath(p);
                        checkAllPlayersDead();
                    }
                } // Gravité vers le haut: mort si trop haut
                else if (p.getGravity() == Gravity.UP && p.getY() + p.getHeight() < 0) {
                    if (p.isAlive()) {
                        p.die();
                        onPlayerDeath(p);
                        checkAllPlayersDead();
                    }
                }
            }

            if (!p.isAlive()) {
                continue;
            }

            // Vérifier si au-dessus d'un trou avec une marge de tolérance
            // On utilise les bords du joueur pour détecter si une partie significative est au-dessus du trou
            boolean overHole = false;
            double playerLeft = p.getX() + 10; // Marge intérieure pour plus de tolérance
            double playerRight = p.getX() + p.getWidth() - 10;
            double playerCenterX = p.getX() + p.getWidth() / 2.0;

            for (Hole h : holes) {
                double holeLeft = h.getX();
                double holeRight = h.getX() + h.getWidth();

                // Le joueur est au-dessus du trou si son centre OU une partie significative est dans le trou
                boolean centerOverHole = playerCenterX > holeLeft && playerCenterX < holeRight;
                boolean significantOverlap = playerLeft < holeRight && playerRight > holeLeft
                        && Math.min(playerRight, holeRight) - Math.max(playerLeft, holeLeft) > p.getWidth() * 0.4;

                if (centerOverHole || significantOverlap) {
                    overHole = true;
                    break;
                }
            }

            if (p.getGravity() == Gravity.DOWN) {
                if (p.getY() + p.getHeight() >= GameConfig.GROUND_Y) {
                    if (overHole) {
                        p.setFalling(true);
                        p.setGrounded(false);
                        // Le joueur tombe dans le trou - animation continue jusqu'à sortie de l'écran
                    } else {
                        // Pas au-dessus d'un trou
                        if (!p.isFalling()) {
                            p.setY(GameConfig.GROUND_Y - p.getHeight());
                            p.setGrounded(true);
                            p.setFalling(false);
                        } else {
                            // On était en train de tomber dans un trou mais on en est sorti -> mort
                            p.die();
                            onPlayerDeath(p);
                            checkAllPlayersDead();
                        }
                    }
                } else {
                    // En l'air mais pas encore au niveau du sol
                    p.setGrounded(false);
                }
            } else {
                // Gravité inversée (vers le haut)
                if (p.getY() <= GameConfig.CEILING_Y) {
                    if (overHole) {
                        p.setFalling(true);
                        p.setGrounded(false);
                        // Le joueur tombe dans le trou - animation continue jusqu'à sortie de l'écran
                    } else {
                        // Pas au-dessus d'un trou
                        if (!p.isFalling()) {
                            p.setY(GameConfig.CEILING_Y);
                            p.setGrounded(true);
                            p.setFalling(false);
                        } else {
                            // On était en train de tomber dans un trou mais on en est sorti -> mort
                            p.die();
                            onPlayerDeath(p);
                            checkAllPlayersDead();
                        }
                    }
                } else {
                    // En l'air mais pas encore au niveau du plafond
                    p.setGrounded(false);
                }
            }
        }
    }

    private void checkAllPlayersDead() {
        // Correction : ne passer en GAME_OVER que si tous les joueurs sont morts
        boolean allDead = true;
        for (Player p : players) {
            if (p.isAlive()) {
                allDead = false;
                break;
            }
        }
        if (allDead) {
            engine.setState(GameState.GAME_OVER);
        }
    }

    // === RENDU ===
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Fond animé
        background.render(g2d);

        // Plateformes (sol et plafond)
        platformRenderer.renderGround(g2d);
        platformRenderer.renderCeiling(g2d);

        // Trous
        for (Hole h : holes) {
            h.render(g2d, GameConfig.GROUND_Y, GameConfig.CEILING_Y, GameConfig.PLATFORM_HEIGHT);
            platformRenderer.renderHoleEdges(g2d, h.getX(), h.getWidth(), true);
            platformRenderer.renderHoleEdges(g2d, h.getX(), h.getWidth(), false);
        }

        // Obstacles
        for (Obstacle obs : obstacles) {
            obs.render(g2d);
        }

        // Ligne d'arrivée
        if (finishLineActive) {
            renderFinishLine(g2d);
        }

        // Tous les joueurs
        for (Player p : players) {
            p.render(g2d);
        }

        // UI en jeu
        renderUI(g2d);

        // Overlay de pause
        if (isPaused) {
            renderPauseOverlay(g2d);
        }

        // Overlay Game Over
        if (isGameOver) {
            renderGameOverOverlay(g2d);
        }

        // Overlay Victoire
        if (isVictory) {
            renderVictoryOverlay(g2d);
        }
    }

    private void renderFinishLine(Graphics2D g2d) {
        int x = (int) finishLineX;

        // Lignes verticales à damier (style drapeau d'arrivée)
        int squareSize = 30;
        for (int row = 0; row < GameConfig.WINDOW_HEIGHT / squareSize + 1; row++) {
            boolean white = row % 2 == 0;
            for (int col = 0; col < 3; col++) {
                if ((row + col) % 2 == 0) {
                    g2d.setColor(Color.WHITE);
                } else {
                    g2d.setColor(Color.BLACK);
                }
                g2d.fillRect(x + col * squareSize, row * squareSize, squareSize, squareSize);
            }
        }

        // Bordure luminescente
        g2d.setColor(GameConfig.NEON_CYAN);
        g2d.setStroke(new BasicStroke(4));
        g2d.drawLine(x - 5, 0, x - 5, GameConfig.WINDOW_HEIGHT);

        // Texte "FINISH"
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.setColor(GameConfig.NEON_GREEN);

        // Texte vertical
        String finish = "F I N I S H";
        for (int i = 0; i < finish.length(); i++) {
            char c = finish.charAt(i);
            if (c != ' ') {
                g2d.drawString(String.valueOf(c), x + squareSize * 3 + 15, 150 + i * 25);
            }
        }
    }

    private void renderUI(Graphics2D g2d) {
        // === PING (MODE RÉSEAU UNIQUEMENT) ===
        if (isNetworkMode()) {
            renderPingIndicator(g2d);
        }

        // Score de tous les joueurs
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        int scoreY = 35;

        // Décaler les scores si on affiche le ping
        if (isNetworkMode()) {
            scoreY = 65;
        }

        for (Player p : players) {
            String scoreText = p.getPlayerName() + ": " + p.getScore();
            if (!p.isAlive()) {
                scoreText += " 💀";
            }

            // Ombre
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.drawString(scoreText, 22, scoreY + 2);

            // Texte avec couleur du joueur
            g2d.setColor(p.getPlayerColor());
            g2d.drawString(scoreText, 20, scoreY);

            scoreY += 28;
        }

        // Instructions (afficher les touches de tous les joueurs)
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.setColor(new Color(150, 150, 150));

        StringBuilder instructions = new StringBuilder();
        List<core.PlayerConfig> configs = engine.getPlayerConfigs();
        if (configs != null && !configs.isEmpty()) {
            for (core.PlayerConfig config : configs) {
                instructions.append("J").append(config.getPlayerId()).append(": ");
                instructions.append(core.PlayerConfig.getKeyName(config.getKeyGravitySwitch()));
                instructions.append("  ");
            }
        } else {
            // Afficher les contrôles par défaut
            instructions.append("CLIC GAUCHE ou ESPACE: Sauter  ");
        }
        instructions.append("| ECHAP: Pause");

        g2d.drawString(instructions.toString(), 20, GameConfig.WINDOW_HEIGHT - 15);
    }

    /**
     * Affiche l'indicateur de ping en mode réseau
     */
    private void renderPingIndicator(Graphics2D g2d) {
        int ping = engine.getPing();

        // Déterminer la couleur selon la qualité du ping
        Color pingColor;
        String quality;
        if (ping < 50) {
            pingColor = GameConfig.NEON_GREEN;
            quality = "Excellent";
        } else if (ping < 100) {
            pingColor = GameConfig.NEON_CYAN;
            quality = "Bon";
        } else if (ping < 200) {
            pingColor = new Color(255, 200, 0); // Orange
            quality = "Moyen";
        } else {
            pingColor = GameConfig.NEON_PINK;
            quality = "Mauvais";
        }

        // Fond semi-transparent
        g2d.setColor(new Color(0, 0, 30, 180));
        g2d.fillRoundRect(15, 10, 120, 40, 10, 10);

        // Bordure
        g2d.setColor(pingColor);
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawRoundRect(15, 10, 120, 40, 10, 10);

        // Icône signal (barres)
        int barsX = 25;
        int barsY = 38;
        for (int i = 0; i < 4; i++) {
            int barHeight = 5 + i * 4;
            int alpha = (ping < (i + 1) * 50) ? 255 : 80;
            g2d.setColor(new Color(pingColor.getRed(), pingColor.getGreen(), pingColor.getBlue(), alpha));
            g2d.fillRect(barsX + i * 6, barsY - barHeight, 4, barHeight);
        }

        // Texte du ping
        g2d.setFont(new Font("Consolas", Font.BOLD, 14));
        g2d.setColor(pingColor);
        g2d.drawString(ping + " ms", 55, 35);

        // Label "PING"
        g2d.setFont(new Font("Arial", Font.PLAIN, 9));
        g2d.setColor(new Color(150, 150, 150));
        g2d.drawString("PING", 55, 22);
    }

    private void renderPauseOverlay(Graphics2D g2d) {
        // Fond semi-transparent
        g2d.setColor(new Color(0, 0, 20, 200));
        g2d.fillRect(0, 0, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);

        // Titre PAUSE
        String pauseText = "PAUSE";
        g2d.setFont(new Font("Arial", Font.BOLD, 72));
        FontMetrics fm = g2d.getFontMetrics();
        int x = (GameConfig.WINDOW_WIDTH - fm.stringWidth(pauseText)) / 2;
        int y = 200;

        // Glow
        for (int i = 15; i > 0; i--) {
            g2d.setColor(new Color(255, 0, 128, (15 - i) * 8));
            g2d.drawString(pauseText, x, y);
        }

        g2d.setColor(GameConfig.NEON_PINK);
        g2d.drawString(pauseText, x, y);

        // Boutons
        for (NeonButton btn : pauseButtons) {
            btn.render(g2d);
        }
    }

    private void renderGameOverOverlay(Graphics2D g2d) {
        // Fond avec animation
        g2d.setColor(new Color(0, 0, 0, (int) (220 * gameOverAlpha)));
        g2d.fillRect(0, 0, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);

        // Appliquer l'échelle
        Graphics2D g2dScaled = (Graphics2D) g2d.create();
        float scale = 0.5f + gameOverScale * 0.5f;
        int centerX = GameConfig.WINDOW_WIDTH / 2;
        int centerY = 200;
        g2dScaled.translate(centerX, centerY);
        g2dScaled.scale(scale, scale);
        g2dScaled.translate(-centerX, -centerY);

        // Titre GAME OVER
        String gameOverText = "GAME OVER";
        g2dScaled.setFont(new Font("Arial", Font.BOLD, 72));
        FontMetrics fm = g2dScaled.getFontMetrics();
        int x = (GameConfig.WINDOW_WIDTH - fm.stringWidth(gameOverText)) / 2;
        int y = 200;

        // Glow rouge
        for (int i = 20; i > 0; i--) {
            int alpha = (int) ((20 - i) * 10 * gameOverAlpha);
            g2dScaled.setColor(new Color(255, 0, 0, alpha));
            g2dScaled.drawString(gameOverText, x, y);
        }

        g2dScaled.setColor(new Color(255, 50, 50, (int) (255 * gameOverAlpha)));
        g2dScaled.drawString(gameOverText, x, y);

        g2dScaled.dispose();

        // Score final
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        String scoreText = "Score final: " + player.getScore();
        fm = g2d.getFontMetrics();
        x = (GameConfig.WINDOW_WIDTH - fm.stringWidth(scoreText)) / 2;

        g2d.setColor(new Color(255, 255, 255, (int) (255 * gameOverAlpha)));
        g2d.drawString(scoreText, x, 280);

        // Boutons (avec alpha)
        if (gameOverAlpha > 0.5f) {
            for (NeonButton btn : gameOverButtons) {
                btn.render(g2d);
            }
        }
    }

    private void renderVictoryOverlay(Graphics2D g2d) {
        // Fond avec animation (doré/brillant)
        g2d.setColor(new Color(20, 15, 0, (int) (220 * victoryAlpha)));
        g2d.fillRect(0, 0, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);

        // Appliquer l'échelle
        Graphics2D g2dScaled = (Graphics2D) g2d.create();
        float scale = 0.5f + victoryScale * 0.5f;
        int centerX = GameConfig.WINDOW_WIDTH / 2;
        int centerY = 200;
        g2dScaled.translate(centerX, centerY);
        g2dScaled.scale(scale, scale);
        g2dScaled.translate(-centerX, -centerY);

        // Titre VICTOIRE
        String victoryText = "VICTOIRE !";
        g2dScaled.setFont(new Font("Arial", Font.BOLD, 72));
        FontMetrics fm = g2dScaled.getFontMetrics();
        int x = (GameConfig.WINDOW_WIDTH - fm.stringWidth(victoryText)) / 2;
        int y = 200;

        // Glow doré
        for (int i = 20; i > 0; i--) {
            int alpha = (int) ((20 - i) * 10 * victoryAlpha);
            g2dScaled.setColor(new Color(255, 215, 0, alpha));
            g2dScaled.drawString(victoryText, x, y);
        }

        g2dScaled.setColor(new Color(255, 223, 0, (int) (255 * victoryAlpha)));
        g2dScaled.drawString(victoryText, x, y);

        g2dScaled.dispose();

        // Nom du vainqueur
        if (winner != null) {
            g2d.setFont(new Font("Arial", Font.BOLD, 36));
            String winnerText = "🏆 " + winner.getPlayerName() + " 🏆";
            fm = g2d.getFontMetrics();
            x = (GameConfig.WINDOW_WIDTH - fm.stringWidth(winnerText)) / 2;
            g2d.setColor(new Color(winner.getPlayerColor().getRed(), winner.getPlayerColor().getGreen(),
                    winner.getPlayerColor().getBlue(), (int) (255 * victoryAlpha)));
            g2d.drawString(winnerText, x, 280);
        }

        // Message de félicitations
        g2d.setFont(new Font("Arial", Font.ITALIC, 24));
        String congrats = "Félicitations, tu as atteint la ligne d'arrivée !";
        fm = g2d.getFontMetrics();
        x = (GameConfig.WINDOW_WIDTH - fm.stringWidth(congrats)) / 2;
        g2d.setColor(new Color(GameConfig.NEON_GREEN.getRed(), GameConfig.NEON_GREEN.getGreen(),
                GameConfig.NEON_GREEN.getBlue(), (int) (255 * victoryAlpha)));
        g2d.drawString(congrats, x, 340);

        // Score du vainqueur
        if (winner != null) {
            g2d.setFont(new Font("Arial", Font.BOLD, 28));
            String scoreText = "Score: " + winner.getScore();
            fm = g2d.getFontMetrics();
            x = (GameConfig.WINDOW_WIDTH - fm.stringWidth(scoreText)) / 2;
            g2d.setColor(new Color(255, 255, 255, (int) (200 * victoryAlpha)));
            g2d.drawString(scoreText, x, 385);
        }

        // Boutons (réutiliser les boutons de game over)
        if (victoryAlpha > 0.5f) {
            for (NeonButton btn : gameOverButtons) {
                btn.render(g2d);
            }
        }
    }
}
