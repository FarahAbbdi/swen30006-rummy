package rummy.strategy;

import ch.aplu.jcardgame.Hand;
import rummy.MeldDetector;
import rummy.Rummy;

import java.util.List;

/**
 * Strategy interface for game mode-specific rules and behaviors.
 *
 * GRASP Pattern: Information Expert
 * Responsibility: Owns knowledge about mode-specific rules (scoring, declarations, card counts)
 *
 * Design Pattern: Strategy
 * Allows runtime selection of game mode behavior without changing controller
 *
 * Design Pattern: Polymorphism
 * Each implementation provides mode-specific behavior through common interface
 */
public interface GameModeStrategy {

    /**
     * Returns the number of cards each player starts with
     */
    int getStartingCardCount();

    /**
     * Returns the name of this game mode
     */
    String getModeName();

    /**
     * Validates if a declaration (Rummy/Gin/Knock) is legal
     * @param hand The hand making the declaration
     * @param player The player index
     * @param declarationType Type of declaration ("RUMMY", "GIN", "KNOCK")
     * @return true if valid, false if invalid
     */
    boolean validateDeclaration(Hand hand, int player, String declarationType);

    /**
     * Calculates scores at end of round based on mode-specific rules
     * @param hands Array of player hands
     * @param analyses Array of meld analyses for each player
     * @param scores Current score array to update
     * @return Index of round winner
     */
    int calculateRoundScores(Hand[] hands, MeldDetector.MeldAnalysis[] analyses, int[] scores);

    /**
     * Checks if a player can make a declaration with their current hand
     * @param hand The player's hand
     * @param threshold Additional threshold (e.g., knock threshold)
     * @param declarationType Type of declaration to check
     * @return true if player can declare
     */
    boolean canDeclare(Hand hand, String declarationType);

    /**
     * Sets up mode-specific UI buttons
     * @param game Reference to main game for button management
     */
    void setupButtons(Rummy game);

    /**
     * Returns whether this mode uses a specific declaration type
     */
    boolean usesDeclarationType(String declarationType);

    /**
     * Checks if a valid declaration has been made in this round
     * @return true if a declaration has been made that should end the round
     */
    boolean hasActiveDeclaration();

    /**
     * Gets the player who made the active declaration
     * @return player index, or -1 if no active declaration
     */
    int getDeclaringPlayer();

    /**
     * Gets the type of active declaration
     * @return declaration type ("RUMMY", "GIN", "KNOCK") or null if none
     */
    String getDeclarationType();

    /**
     * Gets list of declaration types supported by this mode, in priority order
     * @return list of declaration types (e.g., ["GIN", "KNOCK"] or ["RUMMY"])
     */
    List<String> getSupportedDeclarations();
}