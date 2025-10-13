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

        return newAnalysis.getTotalMeldedCards() > originalAnalysis.getTotalMeldedCards();
    }

    // Criterion 2: Decrease minimum rank gap
    private static boolean evaluateCriterion2(Card drawnCard, Hand hand, Deck deck) {
        Suit drawnSuit = (Suit) drawnCard.getSuit();
        int drawnRank = ((Rank) drawnCard.getRank()).getShortHandValue();

        List<Card> sameSuitCards = hand.getCardList().stream()
                .filter(c -> ((Suit) c.getSuit()) == drawnSuit)
                .collect(Collectors.toList());

        if (sameSuitCards.isEmpty()) {
            return true; // Only one card in suit
        }

        // Calculate minimum gap before adding card
        int originalMinGap = calculateMinimumRankGap(sameSuitCards);

        // Add drawn card and recalculate
        sameSuitCards.add(drawnCard);
        int newMinGap = calculateMinimumRankGap(sameSuitCards);

        return newMinGap < originalMinGap;
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

        return drawnSuitCount > currentMaxCount;
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
        return rankCounts.getOrDefault(drawnRank, 0) > 1;
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
            // Shouldn't happen, but fallback
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

            evaluations.put(card, evaluateCard(card, tempHand, deck));
        }

        // Find cards with least criteria satisfied
        int minCriteria = evaluations.values().stream()
                .mapToInt(EvaluationResult::getCriteriaCount)
                .min().orElse(0);

        List<Card> leastCriteriaCards = deadwood.stream()
                .filter(c -> evaluations.get(c).getCriteriaCount() == minCriteria)
                .collect(Collectors.toList());

        if (leastCriteriaCards.size() == 1) {
            return leastCriteriaCards.get(0);
        }

        // Tie-breaking: least frequent suit and highest value
        return selectBestDiscardFromTiedCards(leastCriteriaCards, deadwood);
    }

    private static Card selectBestDiscardFromTiedCards(List<Card> tiedCards, List<Card> allDeadwood) {
        // Count suit frequencies in deadwood
        Map<Suit, Integer> suitFrequencies = new HashMap<>();
        for (Card c : allDeadwood) {
            Suit suit = (Suit) c.getSuit();
            suitFrequencies.put(suit, suitFrequencies.getOrDefault(suit, 0) + 1);
        }

        // Find minimum frequency
        int minFreq = tiedCards.stream()
                .mapToInt(c -> suitFrequencies.get((Suit) c.getSuit()))
                .min().orElse(Integer.MAX_VALUE);

        List<Card> leastFrequentSuitCards = tiedCards.stream()
                .filter(c -> suitFrequencies.get((Suit) c.getSuit()) == minFreq)
                .collect(Collectors.toList());

        // Among least frequent, select highest value
        return leastFrequentSuitCards.stream()
                .max(Comparator.comparingInt(MeldDetector::getCardValue))
                .orElse(tiedCards.get(0));
    }
}