package rummy.smartcomputer;

import ch.aplu.jcardgame.*;
import rummy.*;

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
            System.out.printf("[%s] Drawn: %s | Result: %b\n",
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
    }
}