package rummy;

import ch.aplu.jcardgame.Card;
import ch.aplu.jcardgame.Deck;
import ch.aplu.jcardgame.Hand;

public class ImmediateMeldStrategy implements CardEvaluationStrategy{
    @Override
    public boolean evaluate(Card drawnCard, Hand hand, Deck deck) {
        // Create temporary hand with the drawn card
        Hand tempHand = new Hand(deck);
        for (Card c : hand.getCardList()) {
            tempHand.insert(c, false);
        }
        tempHand.insert(drawnCard, false);

        MeldDetector.MeldAnalysis originalAnalysis = MeldDetector.findBestMelds(hand);
        MeldDetector.MeldAnalysis newAnalysis = MeldDetector.findBestMelds(tempHand);

        boolean result = newAnalysis.getTotalMeldedCards() > originalAnalysis.getTotalMeldedCards();
        System.out.printf("[Criterion 1] Drawn: %s | MeldedCards before: %d, after: %d -> %b\n",
                drawnCard, originalAnalysis.getTotalMeldedCards(), newAnalysis.getTotalMeldedCards(), result);
        return result;
    }

    @Override
    public String getCriterionName() {
        return "ImmediateMeldStrategy";
    }
}
