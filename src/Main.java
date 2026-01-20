
import core.GameEngine;
import javax.swing.SwingUtilities;

/**
 * Point d'entrée principal du jeu VOID RUNNER
 *
 * @author Juan, Harry, Aro, Sedra, Mahery
 * @version 2.0
 */
public class Main {

    public static void main(String[] args) {
        // Optimisations graphiques Java2D
        System.setProperty("sun.java2d.opengl", "true");
        System.setProperty("sun.java2d.d3d", "true");
        System.setProperty("sun.java2d.noddraw", "false");
        System.setProperty("sun.java2d.pmoffscreen", "true");

        SwingUtilities.invokeLater(() -> {
            System.out.println("╔═══════════════════════════════════════════════════════════════╗");
            System.out.println("║              ⚡ VOID RUNNER - Navigate the Void ⚡            ║");
            System.out.println("╠═══════════════════════════════════════════════════════════════╣");
            System.out.println("║  Contrôles (jusqu'à 5 joueurs):                               ║");
            System.out.println("║    • Joueur 1: ESPACE / HAUT                                  ║");
            System.out.println("║    • Joueur 2: W / Z                                          ║");
            System.out.println("║    • Joueur 3: CTRL / SHIFT                                   ║");
            System.out.println("║    • Joueur 4: ALT / ALT GR                                   ║");
            System.out.println("║    • Joueur 5: ENTRÉE                                         ║");
            System.out.println("║    • ECHAP  : Pause                                           ║");
            System.out.println("╚═══════════════════════════════════════════════════════════════╝");
            System.out.println("\n🚀 Lancement du jeu...\n");

            GameEngine game = new GameEngine();
            game.start();
        });
    }
}
