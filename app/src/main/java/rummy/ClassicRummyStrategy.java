package rummy;

import ch.aplu.jcardgame.Hand;
import java.util.Properties;

/**
 * Strategy for Classic Rummy mode (13 cards, Rummy declaration only)
 */
public class ClassicRummyStrategy implements GameModeStrategy {

    private final Properties properties;
    private final int startingCards;

    // Track declaration state for this mode
    private boolean isRummyDeclared = false;
    private int rummyDeclarer = -1;

    public ClassicRummyStrategy(Properties properties) {
        this.properties = properties;
        this.startingCards = Integer.parseInt(properties.getProperty("number_cards", "13"));
    }

    @Override
    public int getStartingCardCount() {
        return startingCards;
    }

    @Override
    public String getModeName() {
        return "Classic Rummy";
    }

    @Override
    public boolean validateDeclaration(Hand hand, int player, String declarationType) {
        if (!"RUMMY".equals(declarationType)) {
            return false; // Classic only supports RUMMY
        }

        // Validate all cards form melds
        boolean allMelded = MeldDetector.allCardsFormedIntoMelds(hand);

        if (allMelded) {
            isRummyDeclared = true;
            rummyDeclarer = player;
            return true;
        } else {
            // Invalid declaration - reset state
            isRummyDeclared = false;
            rummyDeclarer = -1;
            return false;
        }
    }

    @Override
    public int calculateRoundScores(Hand[] hands, MeldDetector.MeldAnalysis[] analyses, int[] scores) {
        int roundWinner = -1;

        if (isRummyDeclared && rummyDeclarer != -1) {
            // Rummy declared scenario
            int opponent = (rummyDeclarer + 1) % 2;
            int pointsEarned = analyses[opponent].getDeadwoodValue();
            scores[rummyDeclarer] += pointsEarned;
            roundWinner = rummyDeclarer;

            System.out.println("Classic Rummy: P" + rummyDeclarer + " wins with Rummy! +" + pointsEarned);

        } else {
            // Stockpile exhausted scenario
            int d0 = analyses[0].getDeadwoodValue();
            int d1 = analyses[1].getDeadwoodValue();

            if (d0 < d1) {
                scores[0] += d1;
                roundWinner = 0;
                System.out.println("Classic Rummy: Stock exhausted, P0 wins +" + d1);
            } else if (d1 < d0) {
                scores[1] += d0;
                roundWinner = 1;
                System.out.println("Classic Rummy: Stock exhausted, P1 wins +" + d0);
            } else {
                System.out.println("Classic Rummy: Stock exhausted, tie - no points");
                roundWinner = 0; // Default to P0 for next round start
            }
        }

        // Reset declaration state for next round
        isRummyDeclared = false;
        rummyDeclarer = -1;

        return roundWinner;
    }

    @Override
    public boolean canDeclare(Hand hand, String declarationType) {
        if (!"RUMMY".equals(declarationType)) {
            return false;
        }

        return MeldDetector.allCardsFormedIntoMelds(hand);
    }

    @Override
    public void setupButtons(Rummy game) {
        // Show Rummy button, hide Gin/Knock buttons
        game.showRummyButton(true);
        game.showGinButtons(false);
    }

    @Override
    public boolean usesDeclarationType(String declarationType) {
        return "RUMMY".equals(declarationType);
    }

    // Getters for declaration state (used by controller)
    public boolean isRummyDeclared() {
        return isRummyDeclared;
    }

    public int getRummyDeclarer() {
        return rummyDeclarer;
    }

    public void setRummyDeclared(boolean declared, int player) {
        this.isRummyDeclared = declared;
        this.rummyDeclarer = player;
    }
}