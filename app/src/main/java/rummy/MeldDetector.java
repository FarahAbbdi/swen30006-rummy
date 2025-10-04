package rummy;

import ch.aplu.jcardgame.Card;
import ch.aplu.jcardgame.Hand;

import java.util.*;

/**
 * Basic structure + card value calculation
 * Responsible for detecting and analyzing melds in a Rummy hand.
 */
public class MeldDetector {

    /**
     * Represents a meld (either a run or a set)
     */
    public static class Meld {
        private final List<Card> cards;
        private final MeldType type;

        public enum MeldType {
            RUN, SET
        }

        public Meld(List<Card> cards, MeldType type) {
            this.cards = new ArrayList<>(cards);
            this.type = type;
        }

        public List<Card> getCards() {
            return new ArrayList<>(cards);
        }

        public MeldType getType() {
            return type;
        }

        public int size() {
            return cards.size();
        }
    }

    /**
     * Result of meld analysis containing the best melds and remaining deadwood
     */
    public static class MeldAnalysis {
        private final List<Meld> melds;
        private final List<Card> deadwood;
        private final int deadwoodValue;

        public MeldAnalysis(List<Meld> melds, List<Card> deadwood) {
            this.melds = melds;
            this.deadwood = deadwood;
            this.deadwoodValue = calculateDeadwoodValue(deadwood);
        }

        public List<Meld> getMelds() {
            return melds;
        }

        public List<Card> getDeadwood() {
            return deadwood;
        }

        public int getDeadwoodValue() {
            return deadwoodValue;
        }

        public int getTotalMeldedCards() {
            return melds.stream().mapToInt(Meld::size).sum();
        }

        private int calculateDeadwoodValue(List<Card> cards) {
            return cards.stream().mapToInt(MeldDetector::getCardValue).sum();
        }
    }

    /**
     * Gets the point value of a card for deadwood calculation
     */
    public static int getCardValue(Card card) {
        Rank rank = (Rank) card.getRank();
        int value = rank.getShortHandValue();

        // J=11, Q=12, K=13 are all worth 10 points
        if (value >= 11) {
            return 10;
        }
        // Ace is worth 1 point
        else if (value == 1) {
            return 1;
        }
        // 2-10 are face value
        else {
            return value;
        }
    }

    /**
     * Find all possible sets (3-4 cards of same rank)
     */
    private static List<Meld> findAllSets(List<Card> cards) {
        List<Meld> sets = new ArrayList<>();

        // Group cards by rank
        Map<Rank, List<Card>> cardsByRank = new HashMap<>();
        for (Card card : cards) {
            Rank rank = (Rank) card.getRank();
            cardsByRank.putIfAbsent(rank, new ArrayList<>());
            cardsByRank.get(rank).add(card);
        }

        // For each rank with 3+ cards, create sets
        for (Map.Entry<Rank, List<Card>> entry : cardsByRank.entrySet()) {
            List<Card> rankCards = entry.getValue();

            if (rankCards.size() == 3) {
                // Set of 3
                sets.add(new Meld(rankCards, Meld.MeldType.SET));
            } else if (rankCards.size() == 4) {
                // Set of 4
                sets.add(new Meld(rankCards, Meld.MeldType.SET));

                // Also add all possible sets of 3 from these 4 cards
                for (int i = 0; i < 4; i++) {
                    List<Card> setOf3 = new ArrayList<>();
                    for (int j = 0; j < 4; j++) {
                        if (i != j) {
                            setOf3.add(rankCards.get(j));
                        }
                    }
                    sets.add(new Meld(setOf3, Meld.MeldType.SET));
                }
            }
        }

        return sets;
    }

    /**
     * Find all possible runs (3+ consecutive cards of same suit)
     */
    private static List<Meld> findAllRuns(List<Card> cards) {
        List<Meld> runs = new ArrayList<>();

        // Group cards by suit
        Map<Suit, List<Card>> cardsBySuit = new HashMap<>();
        for (Card card : cards) {
            Suit suit = (Suit) card.getSuit();
            cardsBySuit.putIfAbsent(suit, new ArrayList<>());
            cardsBySuit.get(suit).add(card);
        }

        // For each suit, find all runs
        for (Map.Entry<Suit, List<Card>> entry : cardsBySuit.entrySet()) {
            List<Card> suitCards = entry.getValue();

            if (suitCards.size() < 3) {
                continue; // Need at least 3 cards for a run
            }

            // Sort by rank value
            suitCards.sort(Comparator.comparingInt(c -> ((Rank)c.getRank()).getShortHandValue()));

            // Find all consecutive runs of length 3 or more
            for (int start = 0; start < suitCards.size(); start++) {
                for (int end = start + 2; end < suitCards.size(); end++) {
                    List<Card> potentialRun = suitCards.subList(start, end + 1);
                    if (isConsecutiveRun(potentialRun)) {
                        runs.add(new Meld(potentialRun, Meld.MeldType.RUN));
                    }
                }
            }
        }

        return runs;
    }

    /**
     * Checks if cards form a consecutive run
     */
    private static boolean isConsecutiveRun(List<Card> cards) {
        if (cards.size() < 3) return false;

        for (int i = 0; i < cards.size() - 1; i++) {
            int currentRank = ((Rank)cards.get(i).getRank()).getShortHandValue();
            int nextRank = ((Rank)cards.get(i + 1).getRank()).getShortHandValue();

            // Must be consecutive (difference of 1)
            if (nextRank - currentRank != 1) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if a meld overlaps with any meld in the list
     */
    private static boolean overlapsWithAny(Meld meld, List<Meld> melds) {
        Set<Card> meldCards = new HashSet<>(meld.getCards());

        for (Meld other : melds) {
            for (Card card : other.getCards()) {
                if (meldCards.contains(card)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Finds all valid (non-overlapping) combinations of melds
     */
    private static List<List<Meld>> findAllValidCombinations(List<Meld> melds) {
        List<List<Meld>> result = new ArrayList<>();
        findCombinationsRecursive(melds, 0, new ArrayList<>(), result);
        return result;
    }

    private static void findCombinationsRecursive(List<Meld> melds, int index,
                                                  List<Meld> current, List<List<Meld>> result) {
        // Add current combination (even if empty)
        result.add(new ArrayList<>(current));

        // Try adding each remaining meld
        for (int i = index; i < melds.size(); i++) {
            Meld meld = melds.get(i);

            // Only add if it doesn't overlap with current combination
            if (!overlapsWithAny(meld, current)) {
                current.add(meld);
                findCombinationsRecursive(melds, i + 1, current, result);
                current.remove(current.size() - 1);
            }
        }
    }

    /**
     * Finds the best combination of non-overlapping melds
     * Prioritises: 1) Maximum cards melded, 2) Minimum deadwood value
     */
    private static List<Meld> findBestMeldCombination(List<Card> allCards, List<Meld> allMelds) {
        List<List<Meld>> validCombinations = findAllValidCombinations(allMelds);

        if (validCombinations.isEmpty()) {
            return new ArrayList<>();
        }

        // Sort by: 1) Most cards melded, 2) Lowest deadwood value
        validCombinations.sort((combo1, combo2) -> {
            int cards1 = combo1.stream().mapToInt(Meld::size).sum();
            int cards2 = combo2.stream().mapToInt(Meld::size).sum();

            if (cards1 != cards2) {
                return Integer.compare(cards2, cards1); // More cards is better
            }

            // Calculate deadwood values for tie-breaking
            Set<Card> melded1 = new HashSet<>();
            for (Meld m : combo1) {
                melded1.addAll(m.getCards());
            }

            Set<Card> melded2 = new HashSet<>();
            for (Meld m : combo2) {
                melded2.addAll(m.getCards());
            }

            int deadwood1 = 0;
            for (Card c : allCards) {
                if (!melded1.contains(c)) {
                    deadwood1 += getCardValue(c);
                }
            }

            int deadwood2 = 0;
            for (Card c : allCards) {
                if (!melded2.contains(c)) {
                    deadwood2 += getCardValue(c);
                }
            }

            return Integer.compare(deadwood1, deadwood2); // Lower deadwood is better
        });

        return validCombinations.get(0);
    }

    /**
     * Complete implementation
     */
    public static MeldAnalysis findBestMelds(Hand hand) {
        List<Card> allCards = new ArrayList<>(hand.getCardList());

        // Find all possible melds
        List<Meld> allSets = findAllSets(allCards);
        List<Meld> allRuns = findAllRuns(allCards);

        List<Meld> allMelds = new ArrayList<>();
        allMelds.addAll(allSets);
        allMelds.addAll(allRuns);

        // Find the best combination
        List<Meld> bestMelds = findBestMeldCombination(allCards, allMelds);

        // Calculate deadwood
        Set<Card> meldedCards = new HashSet<>();
        for (Meld meld : bestMelds) {
            meldedCards.addAll(meld.getCards());
        }

        List<Card> deadwood = new ArrayList<>();
        for (Card card : allCards) {
            if (!meldedCards.contains(card)) {
                deadwood.add(card);
            }
        }

        return new MeldAnalysis(bestMelds, deadwood);
    }

    /**
     * Checks if all cards in a hand form valid melds
     * Used for Rummy/Gin declaration validation
     */
    public static boolean allCardsFormedIntoMelds(Hand hand) {
        MeldAnalysis analysis = findBestMelds(hand);
        return analysis.getDeadwood().isEmpty();
    }
}