package rummy;

import ch.aplu.jcardgame.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles the logic for selecting which card to discard from the hand.
 * Uses evaluation criteria and tie-breaking rules.
 */
public class DiscardSelector {
    private final CardEvaluator evaluator;

    public DiscardSelector(CardEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    /**
     * Select the best card to discard from the hand
     *
     * @param hand The current hand (14 cards including drawn card)
     * @param deck The deck for creating temporary hands
     * @return The card to discard
     */
    public Card selectCardToDiscard(Hand hand, Deck deck) {
        // Step 1: Find best melds and identify deadwood
        MeldDetector.MeldAnalysis analysis = MeldDetector.findBestMelds(hand);
        List<Card> deadwood = analysis.getDeadwood();

        if (deadwood.isEmpty()) {
            System.out.println("[Discard Selection] No deadwood, fallback to first card in hand.");
            return hand.getCardList().get(0);
        }

        System.out.println("[Discard Selection] Deadwood cards: " + deadwood.size());

        // Step 2: Evaluate each deadwood card
        Map<Card, CardEvaluator.EvaluationResult> evaluations = new HashMap<>();

        for (Card card : deadwood) {
            // Create temporary hand WITHOUT this card
            Hand tempHand = new Hand(deck);
            for (Card c : hand.getCardList()) {
                if (!c.equals(card)) {
                    tempHand.insert(c, false);
                }
            }

            // Evaluate what would happen if we kept this card
            CardEvaluator.EvaluationResult eval = evaluator.evaluate(card, tempHand);
            evaluations.put(card, eval);

            // Debug output
            System.out.printf("[Discard Evaluation] Card: %s | Criteria Satisfied: %d\n",
                    cardToString(card), eval.getCriteriaCount());
        }

        // Step 3: Find cards with LEAST criteria satisfied
        int minCriteria = evaluations.values().stream()
                .mapToInt(CardEvaluator.EvaluationResult::getCriteriaCount)
                .min()
                .orElse(0);

        List<Card> leastCriteriaCards = deadwood.stream()
                .filter(c -> evaluations.get(c).getCriteriaCount() == minCriteria)
                .collect(Collectors.toList());

        System.out.printf("[Discard Tie-break] Cards with least criteria (%d): ", minCriteria);
        leastCriteriaCards.forEach(c -> System.out.print(cardToString(c) + " "));
        System.out.println();

        if (leastCriteriaCards.size() == 1) {
            System.out.println("[Discard Selection] Only one card with least criteria: "
                    + cardToString(leastCriteriaCards.get(0)));
            return leastCriteriaCards.get(0);
        }

        // Step 4: Tie-break by LEAST frequent suit
        Map<Suit, Long> suitFrequencies = deadwood.stream()
                .collect(Collectors.groupingBy(c -> (Suit) c.getSuit(), Collectors.counting()));

        long minFreq = leastCriteriaCards.stream()
                .mapToLong(c -> suitFrequencies.get((Suit) c.getSuit()))
                .min()
                .orElse(Long.MAX_VALUE);

        List<Card> leastFrequentSuitCards = leastCriteriaCards.stream()
                .filter(c -> suitFrequencies.get((Suit) c.getSuit()) == minFreq)
                .collect(Collectors.toList());

        System.out.print("[Discard Tie-break] Cards with least frequent suit: ");
        leastFrequentSuitCards.forEach(c -> System.out.print(cardToString(c) + " "));
        System.out.println();

        // Step 5: Final tie-break by HIGHEST card value
        Card selectedCard = leastFrequentSuitCards.stream()
                .max(Comparator.comparingInt(MeldDetector::getCardValue))
                .orElse(leastFrequentSuitCards.get(0));

        System.out.println("[Discard Selection] Card selected (highest value): "
                + cardToString(selectedCard));

        return selectedCard;
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