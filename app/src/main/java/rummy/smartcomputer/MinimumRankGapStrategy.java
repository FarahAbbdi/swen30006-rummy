package rummy.smartcomputer;

import ch.aplu.jcardgame.Card;
import ch.aplu.jcardgame.Deck;
import ch.aplu.jcardgame.Hand;
import rummy.Rank;
import rummy.Suit;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MinimumRankGapStrategy implements CardEvaluationStrategy {
    @Override
    public boolean evaluate(Card drawnCard, Hand hand, Deck deck) {
        Suit drawnSuit = (Suit) drawnCard.getSuit();
        List<Card> handSuitCards = hand.getCardList().stream()
                .filter(c -> ((Suit) c.getSuit()) == drawnSuit)
                .collect(Collectors.toList());

        if (handSuitCards.isEmpty()) {
            System.out.printf("[Criterion 2] Drawn: %s | Only card in suit (TRUE)\n", drawnCard);
            return true;
        }
        // Clone and add drawnCard for the "after" check
        List<Card> withDrawn = new ArrayList<>(handSuitCards);
        withDrawn.add(drawnCard);

        int originalMinGap = calculateMinimumRankGap(handSuitCards);
        int newMinGap = calculateMinimumRankGap(withDrawn);

        boolean result = newMinGap < originalMinGap;
        System.out.printf("[Criterion 2] Drawn: %s | MinGap before: %d, after: %d -> %b\n",
                drawnCard, originalMinGap, newMinGap, result);
        return result;
    }

    private static int calculateMinimumRankGap(List<Card> sameSuitCards) {
        if (sameSuitCards.size() < 2) return Integer.MAX_VALUE;

        List<Integer> ranks = sameSuitCards.stream()
                .map(c -> ((Rank) c.getRank()).getShortHandValue())
                .sorted()
                .collect(Collectors.toList());

        int minGap = Integer.MAX_VALUE;
        for (int i = 0; i < ranks.size() - 1; i++) {
            int gap = ranks.get(i + 1) - ranks.get(i) - 1;
            minGap = Math.min(minGap, gap);
        }
        return minGap;
    }

    @Override
    public String getCriterionName() {
        return "MinimumRankGapStrategy";
    }
}
