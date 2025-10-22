package rummy;

import ch.aplu.jcardgame.Card;
import ch.aplu.jcardgame.Deck;
import ch.aplu.jcardgame.Hand;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeadwoodRankCountStrategy implements CardEvaluationStrategy{
    @Override
    public boolean evaluate(Card drawnCard, Hand hand, Deck deck) {
        // Create temporary hand with drawn card
        Hand tempHand = new Hand(deck);
        for (Card c : hand.getCardList()) {
            tempHand.insert(c, false);
        }
        tempHand.insert(drawnCard, false);

        MeldDetector.MeldAnalysis analysis = MeldDetector.findBestMelds(tempHand);
        List<Card> deadwood = analysis.getDeadwood();

        // Count occurrences of each rank in deadwood
        Map<Rank, Integer> rankCounts = new HashMap<>();
        for (Card c : deadwood) {
            Rank rank = (Rank) c.getRank();
            rankCounts.put(rank, rankCounts.getOrDefault(rank, 0) + 1);
        }

        Rank drawnRank = (Rank) drawnCard.getRank();
        boolean result = rankCounts.getOrDefault(drawnRank, 0) > 1;
        System.out.printf("[Criterion 4] Drawn: %s | Deadwood same rank count: %d -> %b\n",
                drawnCard, rankCounts.getOrDefault(drawnRank, 0), result);
        return result;
    }

    @Override
    public String getCriterionName() {
        return "ImmediateMeldStrategy";
    }
}
