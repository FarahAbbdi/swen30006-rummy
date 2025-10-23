package rummy;

import ch.aplu.jcardgame.Hand;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Strategy for Gin Rummy mode (10 cards, Gin/Knock declarations)
 *
 * GRASP Pattern: Information Expert
 * Owns knowledge of Gin Rummy rules:
 * - 10 cards per player
 * - Gin declaration (all cards in melds)
 * - Knock declaration (anytime, no threshold per spec)
 * - Scoring based on deadwood differences
 */
public class GinRummyStrategy implements GameModeStrategy {

    private final Properties properties;

    // Track declaration state for this mode
    private boolean isGinDeclared = false;
    private int ginDeclarer = -1;
    private boolean isKnockDeclared = false;
    private int knocker = -1;

    public GinRummyStrategy(Properties properties) {
        this.properties = properties;
    }

    @Override
    public int getStartingCardCount() {
        return 10; // Gin Rummy always uses 10 cards
    }

    @Override
    public String getModeName() {
        return "Gin Rummy";
    }

    public boolean isGinMode() {
        return true;
    }

    @Override
    public boolean validateDeclaration(Hand hand, int player, String declarationType) {
        switch (declarationType) {
            case "GIN":
                return validateGin(hand, player);
            case "KNOCK":
                return validateKnock(hand, player);
            default:
                return false;
        }
    }

    private boolean validateGin(Hand hand, int player) {
        // Use facade method for Gin validation
        boolean canDeclare = MeldDetector.canDeclareGin(hand);

        if (canDeclare) {
            isGinDeclared = true;
            ginDeclarer = player;
            System.out.println("VALID GIN by P" + player);
            return true;
        } else {
            int deadwood = MeldDetector.getDeadwoodValue(hand);
            System.out.println("INVALID GIN by P" + player + " - deadwood: " + deadwood);
            isGinDeclared = false;
            ginDeclarer = -1;
            return false;
        }
    }

    private boolean validateKnock(Hand hand, int player) {
        int deadwood = MeldDetector.getDeadwoodValue(hand);

        isKnockDeclared = true;
        knocker = player;
        System.out.println("VALID KNOCK by P" + player + " - deadwood: " + deadwood);
        return true;
    }

    @Override
    public int calculateRoundScores(Hand[] hands, MeldDetector.MeldAnalysis[] analyses, int[] scores) {
        System.out.println("\n=== GIN RUMMY SCORING ===");
        System.out.println("Gin declared: " + isGinDeclared + " by P" + ginDeclarer);
        System.out.println("Knock declared: " + isKnockDeclared + " by P" + knocker);
        System.out.println("P0 deadwood: " + analyses[0].getDeadwoodValue());
        System.out.println("P1 deadwood: " + analyses[1].getDeadwoodValue());

        int roundWinner = -1;

        if (isGinDeclared && ginDeclarer != -1) {
            // Gin declared - winner gets opponent's deadwood + 25 bonus
            int opponent = (ginDeclarer + 1) % 2;
            int opponentDeadwood = analyses[opponent].getDeadwoodValue();
            int points = opponentDeadwood ;
            scores[ginDeclarer] += points;
            roundWinner = ginDeclarer;

            System.out.println("Gin Rummy: P" + ginDeclarer + " wins with GIN! +" + points + " (opponent deadwood: " + opponentDeadwood + ")");

        } else if (isKnockDeclared && knocker != -1) {
            // Knock declared
            int opponent = (knocker + 1) % 2;
            int knockerDeadwood = analyses[knocker].getDeadwoodValue();
            int opponentDeadwood = analyses[opponent].getDeadwoodValue();

            System.out.println("Knocker (P" + knocker + ") deadwood: " + knockerDeadwood);
            System.out.println("Opponent (P" + opponent + ") deadwood: " + opponentDeadwood);

            if (knockerDeadwood < opponentDeadwood) {
                // Successful knock - knocker gets the difference (no bonus)
                int diff = opponentDeadwood - knockerDeadwood;
                scores[knocker] += diff;
                roundWinner = knocker;
                System.out.println("Gin Rummy: Knock success by P" + knocker + " +" + diff);

            } else if (knockerDeadwood > opponentDeadwood) {
                // Undercut! - opponent gets difference + 25 bonus
                int diff = knockerDeadwood - opponentDeadwood;
                int points = diff;
                scores[opponent] += points;
                roundWinner = opponent;
                System.out.println("Gin Rummy: Undercut by P" + opponent + " +" + points + " (diff: " + diff + ")");

            } else {
                // Tie - no points awarded
                System.out.println("Gin Rummy: Knock tie - no points");
                roundWinner = knocker; // Knocker still goes first next round
            }

        } else {
            // Stockpile exhausted - lower deadwood wins opponent's deadwood value
            int d0 = analyses[0].getDeadwoodValue();
            int d1 = analyses[1].getDeadwoodValue();

            if (d0 < d1) {
                scores[0] += d1;
                roundWinner = 0;
                System.out.println("Gin Rummy: Stock exhausted, P0 wins +" + d1);
            } else if (d1 < d0) {
                scores[1] += d0;
                roundWinner = 1;
                System.out.println("Gin Rummy: Stock exhausted, P1 wins +" + d0);
            } else {
                System.out.println("Gin Rummy: Stock exhausted, tie - no points");
                roundWinner = 0;
            }
        }

        System.out.println("Scores after round: P0=" + scores[0] + ", P1=" + scores[1]);
        System.out.println("Round winner: P" + roundWinner);
        System.out.println("=========================\n");

        // Reset declaration state for next round
        isGinDeclared = false;
        ginDeclarer = -1;
        isKnockDeclared = false;
        knocker = -1;

        return roundWinner;
    }

    @Override
    public boolean canDeclare(Hand hand, String declarationType) {
        switch (declarationType) {
            case "GIN":
                return MeldDetector.canDeclareGin(hand);
            case "KNOCK":
                // Per spec: Any player can knock at any time
                return true;
            default:
                return false;
        }
    }

    @Override
    public void setupButtons(Rummy game) {
        // Hide Rummy button, show Gin/Knock buttons
        game.showRummyButton(false);
        game.showGinButtons(true);
    }

    @Override
    public boolean usesDeclarationType(String declarationType) {
        return "GIN".equals(declarationType) || "KNOCK".equals(declarationType);
    }

    // Getters for declaration state
    public boolean isGinDeclared() {
        return isGinDeclared;
    }

    public int getGinDeclarer() {
        return ginDeclarer;
    }

    public boolean isKnockDeclared() {
        return isKnockDeclared;
    }

    public int getKnocker() {
        return knocker;
    }

    public void setGinDeclared(boolean declared, int player) {
        this.isGinDeclared = declared;
        this.ginDeclarer = player;
    }

    public void setKnockDeclared(boolean declared, int player) {
        this.isKnockDeclared = declared;
        this.knocker = player;
    }

    @Override
    public boolean hasActiveDeclaration() {
        return isGinDeclared || isKnockDeclared;
    }

    @Override
    public int getDeclaringPlayer() {
        if (isGinDeclared) return ginDeclarer;
        if (isKnockDeclared) return knocker;
        return -1;
    }

    @Override
    public String getDeclarationType() {
        if (isGinDeclared) return "GIN";
        if (isKnockDeclared) return "KNOCK";
        return null;
    }

    @Override
    public List<String> getSupportedDeclarations() {
        // Gin first (best), then Knock
        return Arrays.asList("GIN", "KNOCK");
    }
}