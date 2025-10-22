package rummy;

import ch.aplu.jcardgame.*;
import java.util.*;

/**
 * Coordinator class that manages all card evaluation strategies.
 * Evaluates cards against multiple criteria and aggregates results.
 */
public class CardEvaluator {
    private final List<CardEvaluationStrategy> strategies;
    private final Deck deck;

    /**
     * Constructor with default strategies (all 4 criteria)
     */
    public CardEvaluator(Deck deck) {
        this.deck = deck;
        this.strategies = new ArrayList<>();

        // Initialize with all 4 default strategies
        strategies.add(new ImmediateMeldStrategy());
        strategies.add(new MinimumRankGapStrategy());
        strategies.add(new MaximumSuitCountStrategy());
        strategies.add(new DeadwoodRankCountStrategy());
    }

    /**
     * Constructor with custom strategies (for extensibility)
     */
    public CardEvaluator(Deck deck, List<CardEvaluationStrategy> strategies) {
        this.deck = deck;
        this.strategies = new ArrayList<>(strategies);
    }

    /**
     * Add a new strategy dynamically
     */
    public void addStrategy(CardEvaluationStrategy strategy) {
        strategies.add(strategy);
    }

    /**
     * Evaluate a card against all strategies
     *
     * @param drawnCard The card to evaluate
     * @param hand The current hand (without drawn card)
     * @return EvaluationResult containing all criteria results
     */
    public EvaluationResult evaluate(Card drawnCard, Hand hand) {
        boolean[] criteriaResults = new boolean[strategies.size()];

        for (int i = 0; i < strategies.size(); i++) {
            CardEvaluationStrategy strategy = strategies.get(i);
            criteriaResults[i] = strategy.evaluate(drawnCard, hand, deck);

            // Debug logging
            System.out.printf("[%s] Drawn: %s | Result: %b | %s\n",
                    strategy.getCriterionName(),
                    cardToString(drawnCard),
                    criteriaResults[i]);
        }

        return new EvaluationResult(criteriaResults);
    }

    /**
     * Helper method to convert card to string for logging
     */
    private String cardToString(Card card) {
        Rank rank = (Rank) card.getRank();
        Suit suit = (Suit) card.getSuit();
        return rank.getCardLog() + suit.getSuitShortHand();
    }

    /**
     * Result class containing evaluation outcomes
     */
    public static class EvaluationResult {
        private final boolean[] criteriaResults;

        public EvaluationResult(boolean[] criteriaResults) {
            this.criteriaResults = Arrays.copyOf(criteriaResults, criteriaResults.length);
        }

        /**
         * Check if any criterion is satisfied
         */
        public boolean satisfiesAnyCriterion() {
            for (boolean result : criteriaResults) {
                if (result) return true;
            }
            return false;
        }

        /**
         * Count how many criteria are satisfied
         */
        public int getCriteriaCount() {
            int count = 0;
            for (boolean result : criteriaResults) {
                if (result) count++;
            }
            return count;
        }

        /**
         * Get result for specific criterion (0-indexed)
         */
        public boolean getCriterion(int index) {
            if (index < 0 || index >= criteriaResults.length) {
                throw new IndexOutOfBoundsException("Invalid criterion index: " + index);
            }
            return criteriaResults[index];
        }

        /**
         * Get all results as array
         */
        public boolean[] getAllResults() {
            return Arrays.copyOf(criteriaResults, criteriaResults.length);
        }

        // Backward compatibility methods (if needed for existing code)
        public boolean criterion1() {
            return criteriaResults.length > 0 ? criteriaResults[0] : false;
        }

        public boolean criterion2() {
            return criteriaResults.length > 1 ? criteriaResults[1] : false;
        }

        public boolean criterion3() {
            return criteriaResults.length > 2 ? criteriaResults[2] : false;
        }

        public boolean criterion4() {
            return criteriaResults.length > 3 ? criteriaResults[3] : false;
        }
    }
}