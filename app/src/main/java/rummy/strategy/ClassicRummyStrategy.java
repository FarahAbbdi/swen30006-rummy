package rummy.strategy;

import ch.aplu.jcardgame.Hand;
import rummy.MeldDetector;
import rummy.Rummy;

import java.util.List;
import java.util.Properties;

/**
 * Strategy for Classic Rummy mode (13 cards, Rummy declaration only)
 * Owns knowledge of Classic Rummy rules:
 * - 13 cards per player
 * - Rummy declaration when all cards form melds
 * - Winner earns opponent's deadwood value
 */
public class ClassicRummyStrategy implements GameModeStrategy {

    private final int startingCards;

    // Track declaration state for this mode
    private boolean isRummyDeclared = false;
    private int rummyDeclarer = -1;

    public ClassicRummyStrategy(Properties properties) {
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

        // Use facade method for validation
        boolean canDeclare = MeldDetector.canDeclareRummy(hand);

        if (canDeclare) {
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
    public int calculateRoundScores(Hand[] hands, int[] scores, boolean stockExhausted) {
        MeldDetector.MeldAnalysis[] analyses = new MeldDetector.MeldAnalysis[hands.length];
        for (int i = 0; i < hands.length; i++) {
            analyses[i] = MeldDetector.findBestMelds(hands[i]);
            System.out.println("P" + i + " " + MeldDetector.getMeldSummary(hands[i]));
        }

        int roundWinner;

        if (isRummyDeclared && rummyDeclarer != -1) {
            // Rummy declared scenario
            int opponent = (rummyDeclarer + 1) % 2;
            int pointsEarned = analyses[opponent].getDeadwoodValue();
            scores[rummyDeclarer] += pointsEarned;
            roundWinner = rummyDeclarer;

            System.out.println("Classic Rummy: P" + rummyDeclarer + " wins with Rummy! +" + pointsEarned);

        } else if (stockExhausted) {
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
        } else {
            System.out.println("Classic Rummy: Round ended with no valid conclusion");
            roundWinner = 0;
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

        return MeldDetector.canDeclareRummy(hand);
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

    @Override
    public boolean hasActiveDeclaration() {
        return isRummyDeclared;
    }

    @Override
    public int getDeclaringPlayer() {
        return rummyDeclarer;
    }

    @Override
    public String getDeclarationType() {
        return isRummyDeclared ? "RUMMY" : null;
    }

    @Override
    public List<String> getSupportedDeclarations() {
        return List.of("RUMMY");
    }
}