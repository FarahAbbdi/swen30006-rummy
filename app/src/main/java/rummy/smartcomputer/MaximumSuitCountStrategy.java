package rummy.smartcomputer;

import ch.aplu.jcardgame.Card;
import ch.aplu.jcardgame.Deck;
import ch.aplu.jcardgame.Hand;
import rummy.Suit;

import java.util.HashMap;
import java.util.Map;

public class MaximumSuitCountStrategy implements CardEvaluationStrategy {
    @Override
    public boolean evaluate(Card drawnCard, Hand hand, Deck deck) {
        Suit drawnSuit = (Suit) drawnCard.getSuit();

        Map<Suit, Integer> suitCounts = new HashMap<>();
        for (Card c : hand.getCardList()) {
            Suit suit = (Suit) c.getSuit();
            suitCounts.put(suit, suitCounts.getOrDefault(suit, 0) + 1);
        }

        int currentMaxCount = suitCounts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        int drawnSuitCount = suitCounts.getOrDefault(drawnSuit, 0) + 1;

        boolean result = drawnSuitCount > currentMaxCount;
        System.out.printf("[Criterion 3] Drawn: %s | Suit: %s | Count before: %d, after: %d -> %b\n",
                drawnCard, drawnSuit, suitCounts.getOrDefault(drawnSuit, 0), drawnSuitCount, result);
        return result;
    }

    @Override
    public String getCriterionName() {
        return "MaximumSuitCountStrategy";
    }
}
