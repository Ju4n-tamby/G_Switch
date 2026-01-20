package ui;

import core.GameConfig;
import javax.swing.*;
import java.awt.*;

/**
 * Fenêtre principale du jeu (optimisée pour les performances)
 */
public class GameWindow extends JFrame {
    
    public GameWindow(JPanel contentPanel) {
        setTitle(GameConfig.GAME_TITLE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        
        // Configurer le contenu
        setContentPane(contentPanel);
        
        // Taille de la fenêtre
        setPreferredSize(new Dimension(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT));
        pack();
        
        // Centrer la fenêtre
        setLocationRelativeTo(null);
        
        // Style de la fenêtre
        setBackground(GameConfig.DARK_BG);
        
        // Optimisation: ignorer le repaint système pour contrôler le rendu
        setIgnoreRepaint(false);
    }
}
