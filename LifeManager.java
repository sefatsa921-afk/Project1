import javax.swing.JOptionPane;

public class LifeManager {

    private int lives;

    public LifeManager(int startingLives) {
        this.lives = startingLives;
    }

    public int getLives() {
        return this.lives;
    }

    public void setLives(int lives) {
        this.lives = lives;
    }

    public boolean deductLife(String reason) {
        if (this.lives <= 0) {
            return false;
        }

        this.lives = this.lives - 1;

        if ("HINT".equalsIgnoreCase(reason)) {
            System.out.println("Hint used! 1 life lost. Remaining lives: " + this.lives);
        } else if ("MISTAKE".equalsIgnoreCase(reason)) {
            System.out.println("Mistake made! 1 life lost. Remaining lives: " + this.lives);
        } else {
            System.out.println("1 life lost. Remaining lives: " + this.lives);
        }

        if (this.lives <= 0) {
            triggerGameOver();
            return false;
        }

        return true;
    }

    private void triggerGameOver() {
        JOptionPane.showMessageDialog(
            null, 
            "Game Over! You ran out of lives.", 
            "Futoshiki Game", 
            JOptionPane.ERROR_MESSAGE
        );
    }


    public static void main(String[] args) {

        LifeManager gameLives = new LifeManager(3);

        System.out.println("Starting lives: " + gameLives.getLives());

        gameLives.deductLife("HINT");

        gameLives.deductLife("MISTAKE");
        
        gameLives.deductLife("MISTAKE");
    }
}