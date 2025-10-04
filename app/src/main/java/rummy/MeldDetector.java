package rummy;

import ch.aplu.jcardgame.Card;
import ch.aplu.jcardgame.Hand;

import java.util.*;

/**
 * Basic structure + card value calculation
 * Responsible for detecting and analysing melds in a Rummy hand.
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

    public static MeldAnalysis findBestMelds(Hand hand) {
        // For now, return empty melds (all cards are deadwood)
        List<Card> allCards = new ArrayList<>(hand.getCardList());
        return new MeldAnalysis(new ArrayList<>(), allCards);
    }

    public static boolean allCardsFormedIntoMelds(Hand hand) {
        MeldAnalysis analysis = findBestMelds(hand);
        return analysis.getDeadwood().isEmpty();
    }
}