package ui.components;

import core.GameConfig;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 * Zone de chat pour le mode multijoueur réseau uniquement
 * S'active automatiquement quand un joueur meurt
 */
public class ChatPanel extends JPanel {
    
    private JTextArea chatArea;
    private JTextField inputField;
    private JScrollPane scrollPane;
    private List<ChatMessage> messages;
    private boolean isEnabled;      // Chat actif (mode réseau)
    private boolean isInputActive;  // Joueur peut taper (mort)
    private String playerName;
    
    // Callbacks
    private ChatCallback callback;
    
    public interface ChatCallback {
        void onMessageSent(String playerName, String message);
    }
    
    public ChatPanel() {
        this.messages = new ArrayList<>();
        this.isEnabled = false;
        this.isInputActive = false;
        this.playerName = "Player";
        setupUI();
    }
    
    private void setupUI() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setPreferredSize(new Dimension(300, 200));
        
        // Zone d'affichage des messages
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(new Color(10, 10, 30, 200));
        chatArea.setForeground(GameConfig.NEON_CYAN);
        chatArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        scrollPane = new JScrollPane(chatArea);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createLineBorder(GameConfig.NEON_CYAN, 1));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        // Champ de saisie
        inputField = new JTextField();
        inputField.setBackground(new Color(20, 20, 40));
        inputField.setForeground(Color.WHITE);
        inputField.setCaretColor(GameConfig.NEON_CYAN);
        inputField.setFont(new Font("Consolas", Font.PLAIN, 12));
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GameConfig.NEON_CYAN, 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        inputField.setEnabled(false); // Désactivé par défaut
        
        inputField.addActionListener(e -> sendMessage());
        
        add(scrollPane, BorderLayout.CENTER);
        add(inputField, BorderLayout.SOUTH);
        
        // Masqué par défaut
        setVisible(false);
    }
    
    /**
     * Active le chat (mode réseau uniquement)
     */
    public void enableChat(boolean enabled) {
        this.isEnabled = enabled;
        setVisible(enabled);
        if (!enabled) {
            isInputActive = false;
            inputField.setEnabled(false);
        }
    }
    
    /**
     * Active la saisie pour un joueur mort
     */
    public void activateInput(String deadPlayerName) {
        if (!isEnabled) return;
        
        this.playerName = deadPlayerName;
        this.isInputActive = true;
        inputField.setEnabled(true);
        inputField.setBackground(new Color(30, 20, 40));
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GameConfig.NEON_PINK, 2),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        inputField.requestFocusInWindow();
        
        addSystemMessage("💀 " + deadPlayerName + " peut maintenant discuter");
    }
    
    /**
     * Désactive la saisie (joueur respawn ou fin de partie)
     */
    public void deactivateInput() {
        this.isInputActive = false;
        inputField.setEnabled(false);
        inputField.setBackground(new Color(20, 20, 40));
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GameConfig.NEON_CYAN, 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
    }
    
    /**
     * Envoie un message
     */
    private void sendMessage() {
        if (!isInputActive) return;
        
        String text = inputField.getText().trim();
        if (!text.isEmpty()) {
            addMessage(playerName, text, GameConfig.NEON_PINK);
            if (callback != null) {
                callback.onMessageSent(playerName, text);
            }
            inputField.setText("");
        }
    }
    
    /**
     * Ajoute un message au chat
     */
    public void addMessage(String sender, String message, Color color) {
        if (!isEnabled) return;
        
        ChatMessage msg = new ChatMessage(sender, message, color);
        messages.add(msg);
        
        // Mettre à jour l'affichage
        chatArea.append("[" + sender + "]: " + message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
    
    /**
     * Ajoute un message système
     */
    public void addSystemMessage(String message) {
        if (!isEnabled) return;
        
        chatArea.append(">> " + message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
    
    /**
     * Efface tous les messages
     */
    public void clearMessages() {
        messages.clear();
        chatArea.setText("");
    }
    
    /**
     * Reset le chat pour une nouvelle partie
     */
    public void reset() {
        clearMessages();
        deactivateInput();
    }
    
    public void setCallback(ChatCallback callback) {
        this.callback = callback;
    }
    
    public boolean isInputActive() {
        return isInputActive;
    }
    
    public boolean isChatEnabled() {
        return isEnabled;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        if (!isEnabled) return;
        
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Fond semi-transparent
        g2d.setColor(new Color(10, 10, 30, 180));
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
        
        // Bordure
        Color borderColor = isInputActive ? GameConfig.NEON_PINK : GameConfig.NEON_CYAN;
        g2d.setColor(borderColor);
        g2d.setStroke(new BasicStroke(isInputActive ? 2 : 1));
        g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
        
        // Indicateur "CHAT RÉSEAU" en haut
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        g2d.setColor(GameConfig.NEON_CYAN);
        g2d.drawString("💬 CHAT RÉSEAU", 8, -5);
        
        super.paintComponent(g);
    }
    
    // === CLASSE INTERNE CHATMESSAGE ===
    
    private static class ChatMessage {
        String sender;
        String message;
        Color color;
        long timestamp;
        
        ChatMessage(String sender, String message, Color color) {
            this.sender = sender;
            this.message = message;
            this.color = color;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
