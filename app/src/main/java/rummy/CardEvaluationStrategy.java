package rummy;

import ch.aplu.jcardgame.*;

/**
 * Strategy interface for evaluating whether a card should be kept by the smart computer player.
 * Each concrete strategy implements one of the four evaluation criteria.
 */
public interface CardEvaluationStrategy {

    /**
     * Evaluates whether the drawn card satisfies this criterion
     *
     * @param drawnCard The card being evaluated
     * @param hand The current hand (without the drawn card)
     * @param deck The deck used for creating temporary hands
     * @return true if criterion is satisfied, false otherwise
     */
    boolean evaluate(Card drawnCard, Hand hand, Deck deck);

    /**
     * @return The name of this criterion (e.g., "Criterion 1")
     */
    String getCriterionName();
}