package graphics;

import core.GameConfig;
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Système de particules pour les effets visuels
 */
public class ParticleSystem {
    
    private List<Particle> particles;
    
    public ParticleSystem() {
        this.particles = new ArrayList<>();
    }
    
    /**
     * Met à jour toutes les particules
     */
    public void update() {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.update();
            if (p.isDead()) {
                it.remove();
            }
        }
    }
    
    /**
     * Dessine toutes les particules
     */
    public void render(Graphics2D g2d) {
        for (Particle p : particles) {
            p.render(g2d);
        }
    }
    
    /**
     * Émet des particules de mort
     */
    public void emitDeath(double x, double y, Color color) {
        for (int i = 0; i < GameConfig.DEATH_PARTICLE_COUNT; i++) {
            double angle = Math.random() * Math.PI * 2;
            double speed = 2 + Math.random() * 8;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed;
            
            int size = 3 + (int) (Math.random() * 8);
            Color particleColor = varyColor(color);
            
            particles.add(new Particle(x, y, vx, vy, size, particleColor, 
                                      40 + (int) (Math.random() * 40), ParticleType.EXPLOSION));
        }
    }
    
    /**
     * Émet des particules lors du changement de gravité
     */
    public void emitGravitySwitch(double x, double y, Color color) {
        for (int i = 0; i < 20; i++) {
            double angle = Math.random() * Math.PI * 2;
            double speed = 1 + Math.random() * 4;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed;
            
            int size = 2 + (int) (Math.random() * 5);
            
            particles.add(new Particle(x, y, vx, vy, size, color, 
                                      20 + (int) (Math.random() * 20), ParticleType.GLOW));
        }
    }
    
    /**
     * Émet des particules de traînée
     */
    public void emitTrail(double x, double y, Color color) {
        if (Math.random() < 0.3) { // Émettre seulement parfois
            double vx = -1 - Math.random();
            double vy = (Math.random() - 0.5) * 2;
            int size = 2 + (int) (Math.random() * 3);
            
            Color trailColor = new Color(color.getRed(), color.getGreen(), 
                                        color.getBlue(), 150);
            
            particles.add(new Particle(x, y, vx, vy, size, trailColor, 
                                      15, ParticleType.TRAIL));
        }
    }
    
    /**
     * Vide toutes les particules
     */
    public void clear() {
        particles.clear();
    }
    
    /**
     * Varie légèrement une couleur
     */
    private Color varyColor(Color base) {
        int r = clamp(base.getRed() + (int) (Math.random() * 60 - 30), 0, 255);
        int g = clamp(base.getGreen() + (int) (Math.random() * 60 - 30), 0, 255);
        int b = clamp(base.getBlue() + (int) (Math.random() * 60 - 30), 0, 255);
        return new Color(r, g, b);
    }
    
    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
    
    // === CLASSE INTERNE PARTICLE ===
    
    private enum ParticleType {
        EXPLOSION, GLOW, TRAIL
    }
    
    private class Particle {
        double x, y;
        double vx, vy;
        int size;
        Color color;
        int life;
        int maxLife;
        ParticleType type;
        
        Particle(double x, double y, double vx, double vy, int size, 
                Color color, int life, ParticleType type) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.color = color;
            this.life = life;
            this.maxLife = life;
            this.type = type;
        }
        
        void update() {
            x += vx;
            y += vy;
            
            // Friction
            vx *= 0.98;
            vy *= 0.98;
            
            // Gravité légère pour les explosions
            if (type == ParticleType.EXPLOSION) {
                vy += 0.1;
            }
            
            life--;
            
            // Réduire la taille progressivement
            if (life < maxLife / 3 && size > 1) {
                size--;
            }
        }
        
        boolean isDead() {
            return life <= 0;
        }
        
        void render(Graphics2D g2d) {
            float alpha = (float) life / maxLife;
            Color renderColor = new Color(color.getRed(), color.getGreen(),
                                         color.getBlue(), (int) (alpha * 255));
            
            g2d.setColor(renderColor);
            
            switch (type) {
                case EXPLOSION:
                    // Particules carrées rotatives
                    g2d.fillRect((int) x - size/2, (int) y - size/2, size, size);
                    break;
                case GLOW:
                    // Cercles avec glow
                    g2d.fillOval((int) x - size/2, (int) y - size/2, size, size);
                    g2d.setColor(new Color(255, 255, 255, (int) (alpha * 100)));
                    g2d.fillOval((int) x - size/4, (int) y - size/4, size/2, size/2);
                    break;
                case TRAIL:
                    // Petits cercles
                    g2d.fillOval((int) x - size/2, (int) y - size/2, size, size);
                    break;
            }
        }
    }
}
