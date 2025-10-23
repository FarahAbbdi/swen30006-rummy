package rummy.smartcomputer;

import ch.aplu.jcardgame.*;
import rummy.Rank;
import rummy.Suit;

/**
 * Smart computer player that uses evaluation strategies to make intelligent decisions.
 * Acts as a facade for the card evaluation and discard selection subsystem.
 */
public class SmartComputerPlayer {
    private final CardEvaluator evaluator;
    private final DiscardSelector discardSelector;

    /**
     * Constructor with default configuration
     */
    public SmartComputerPlayer(Deck deck) {
        this.evaluator = new CardEvaluator(deck);
        this.discardSelector = new DiscardSelector(evaluator);
    }

    /**
     * Simple boolean check: should the computer keep this card?
     *
     * @param drawnCard The card being evaluated
     * @param hand The current hand (without drawn card)
     * @return true if any criterion is satisfied, false otherwise
     */
    public boolean shouldKeepCard(Card drawnCard, Hand hand) {
        CardEvaluator.EvaluationResult result = evaluator.evaluate(drawnCard, hand);
        boolean shouldKeep = result.satisfiesAnyCriterion();

        System.out.printf("[Smart Player] Card: %s | Should Keep: %b (Criteria: %d/4)\n",
                cardToString(drawnCard), shouldKeep, result.getCriteriaCount());

        return shouldKeep;
    }

    /**
     * Select which card to discard from the hand
     *
     * @param hand The current hand (14 cards including drawn card)
     * @return The card to discard
     */
    public Card selectCardToDiscard(Hand hand, Deck deck) {
        return discardSelector.selectCardToDiscard(hand, deck);
    }

    /**
     * Helper method to convert card to string for logging
     */
    private String cardToString(Card card) {
        Rank rank = (Rank) card.getRank();
        Suit suit = (Suit) card.getSuit();
        return rank.getCardLog() + suit.getSuitShortHand();
    }
}