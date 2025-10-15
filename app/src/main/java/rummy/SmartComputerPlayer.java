package rummy;

import ch.aplu.jcardgame.*;
import java.util.*;
import java.util.stream.Collectors;

public class SmartComputerPlayer {

    public static class EvaluationResult {
        public final boolean criterion1; // Immediate meld form
        public final boolean criterion2; // Decrease minimum rank gap
        public final boolean criterion3; // Increase max cards of suit
        public final boolean criterion4; // Make suit count > 1 in deadwood

        public EvaluationResult(boolean c1, boolean c2, boolean c3, boolean c4) {
            this.criterion1 = c1;
            this.criterion2 = c2;
            this.criterion3 = c3;
            this.criterion4 = c4;
        }

        public boolean satisfiesAnyCriterion() {
            return criterion1 || criterion2 || criterion3 || criterion4;
        }

        public int getCriteriaCount() {
            return (criterion1 ? 1 : 0) + (criterion2 ? 1 : 0) +
                    (criterion3 ? 1 : 0) + (criterion4 ? 1 : 0);
        }
    }

    // Criterion 1: Immediate meld form
    private static boolean evaluateCriterion1(Card drawnCard, Hand hand, Deck deck) {
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

    // Criterion 2: Decrease minimum rank gap
    private static boolean evaluateCriterion2(Card drawnCard, Hand hand, Deck deck) {
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

    // Criterion 3: Increase max cards of suit
    private static boolean evaluateCriterion3(Card drawnCard, Hand hand, Deck deck) {
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

    // Criterion 4: Make suit count > 1 in deadwood
    private static boolean evaluateCriterion4(Card drawnCard, Hand hand, Deck deck) {
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

    public static EvaluationResult evaluateCard(Card drawnCard, Hand hand, Deck deck) {
        boolean c1 = evaluateCriterion1(drawnCard, hand, deck);
        boolean c2 = evaluateCriterion2(drawnCard, hand, deck);
        boolean c3 = evaluateCriterion3(drawnCard, hand, deck);
        boolean c4 = evaluateCriterion4(drawnCard, hand, deck);

        return new EvaluationResult(c1, c2, c3, c4);
    }

    public static Card selectCardToDiscard(Hand hand, Deck deck) {
        MeldDetector.MeldAnalysis analysis = MeldDetector.findBestMelds(hand);
        List<Card> deadwood = analysis.getDeadwood();

        if (deadwood.isEmpty()) {
            System.out.println("[Discard Selection] No deadwood, fallback to first card in hand.");
            return hand.getCardList().get(0);
        }

        // Evaluate each deadwood card
        Map<Card, EvaluationResult> evaluations = new HashMap<>();
        for (Card card : deadwood) {
            // Create hand without this card to evaluate
            Hand tempHand = new Hand(deck);
            for (Card c : hand.getCardList()) {
                if (!c.equals(card)) {
                    tempHand.insert(c, false);
                }
            }
            EvaluationResult eval = evaluateCard(card, tempHand, deck);
            evaluations.put(card, eval);

            // Debug output for the evaluation
            System.out.printf("[Discard Evaluation] Card: %s | Criteria Satisfied: %d | C1: %b | C2: %b | C3: %b | C4: %b\n",
                    card, eval.getCriteriaCount(), eval.criterion1, eval.criterion2, eval.criterion3, eval.criterion4);
        }

        // Find cards with least criteria satisfied
        int minCriteria = evaluations.values().stream()
                .mapToInt(EvaluationResult::getCriteriaCount)
                .min().orElse(0);

        List<Card> leastCriteriaCards = deadwood.stream()
                .filter(c -> evaluations.get(c).getCriteriaCount() == minCriteria)
                .collect(Collectors.toList());

        // Debug output for tie-breaking candidates
        System.out.print("[Discard Tie-break] Cards with least criteria: ");
        leastCriteriaCards.forEach(c -> System.out.print(c + " "));
        System.out.println();

        if (leastCriteriaCards.size() == 1) {
            System.out.println("[Discard Selection] Only one card with least criteria: " + leastCriteriaCards.get(0));
            return leastCriteriaCards.get(0);
        }

        // Tie-breaking: least frequent suit
        Map<Suit, Long> suitFrequencies = deadwood.stream()
                .collect(Collectors.groupingBy(c -> (Suit) c.getSuit(), Collectors.counting()));

        long minFreq = leastCriteriaCards.stream()
                .mapToLong(c -> suitFrequencies.get((Suit) c.getSuit()))
                .min().orElse(Long.MAX_VALUE);

        List<Card> leastFrequentSuitCards = leastCriteriaCards.stream()
                .filter(c -> suitFrequencies.get((Suit) c.getSuit()) == minFreq)
                .collect(Collectors.toList());

        // Debug output for suit frequency tie-breaking
        System.out.print("[Discard Tie-break] Cards with least frequent suit: ");
        leastFrequentSuitCards.forEach(c -> System.out.print(c + " "));
        System.out.println();

        // Highest card value among the tied cards
        Card highestValueCard = leastFrequentSuitCards.stream()
                .max(Comparator.comparingInt(MeldDetector::getCardValue))
                .orElse(leastFrequentSuitCards.get(0));

        System.out.println("[Discard Selection] Card selected (highest value among tie): " + highestValueCard);

        return highestValueCard;
    }
}