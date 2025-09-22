package rummy;

public enum Rank implements Comparable<Rank> {
    ACE(1), KING(13), QUEEN(12), JACK(11),
    TEN(10), NINE(9), EIGHT(8), SEVEN(7), SIX(6),
    FIVE(5), FOUR(4), THREE(3), TWO(2);

    private int shortHandValue = 0;

    Rank(int shortHandValue) {
        this.shortHandValue = shortHandValue;
    }

    public int getShortHandValue() {
        return shortHandValue;
    }

    public String getCardLog() {
        return String.format("%d", shortHandValue);
    }
}