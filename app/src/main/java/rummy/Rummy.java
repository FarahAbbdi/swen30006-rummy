package rummy;

import ch.aplu.jcardgame.*;
import ch.aplu.jgamegrid.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("serial")
public class Rummy extends CardGame {
    // ===== Classic Rummy state =====
    private boolean isRummyDeclared = false;
    private int rummyDeclarer = -1;

    // ===== Gin Rummy state =====
    private boolean isGinDeclared = false;
    private int ginDeclarer = -1;
    private boolean isKnockDeclared = false;
    private int knocker = -1;
    private final int knockThreshold; // default 7 (spec)

    // ===== Mode =====
    private final boolean isGinMode;

    static public final int seed = 30008;
    static final Random random = new Random(seed);
    private final Properties properties;
    private final StringBuilder logResult = new StringBuilder();
    private final List<List<String>> playerAutoMovements = new ArrayList<>();

    private final String version = "1.0";
    public final int nbPlayers = 2;
    public int nbStartCards = 13;
    private final int handWidth = 400;
    private final int pileWidth = 40;
    private final int cardWidth = 40;
    private int thinkingTime = 300;

    private final Deck deck = new Deck(Suit.values(), Rank.values(), "cover");
    private final Location[] handLocations = {
            new Location(350, 75),
            new Location(350, 625),
    };

    private Hand pack;
    private Hand discard;

    // === Buttons ===
    private final GGButton endTurnActor = new GGButton("sprites/end.gif", false);
    private final Location endTurnLocation = new Location(80, 610);

    // Classic Rummy button
    private final GGButton rummyActor = new GGButton("sprites/rummy.gif", false);
    private final Location rummyLocation = new Location(80, 650);

    // Gin & Knock buttons (aligned and spaced)
    private final GGButton ginActor   = new GGButton("sprites/gin.gif", false);
    private final GGButton knockActor = new GGButton("sprites/knock.gif", false);

    // Keep them aligned horizontally and same height as Rummy
    private final Location ginLocation   = new Location(80, 570);
    private final Location knockLocation = new Location(80, 650);  // shifted right

    private TextActor packNameActor;
    private TextActor discardNameActor;

    private final Location packLocation = new Location(75, 350);
    private final Location discardLocation = new Location(625, 350);
    private final Location packNameLocation = new Location(30, 280);
    private final Location discardNameLocation = new Location(560, 280);

    private final Location[] scoreLocations = {
            new Location(25, 25),
            new Location(575, 675),
    };

    private final Location[] pileNameLocations = {
            new Location(25, 50),
            new Location(575, 625),
    };

    enum CardAction {
        DISCARD,
        STOCKPILE,
        RUMMY,
        GIN,
        KNOCK,
        NONE;
    }
    private final TextActor[] scoreActors = {null, null};
    private final TextActor[] pileNameActors = {null, null, null, null};

    Font bigFont = new Font("Arial", Font.BOLD, 36);
    Font smallFont = new Font("Arial", Font.BOLD, 18);

    private final int COMPUTER_PLAYER_INDEX = 0;
    private final int HUMAN_PLAYER_INDEX = 1;
    private int roundWinner = HUMAN_PLAYER_INDEX;

    private final Location playingLocation = new Location(350, 350);
    private final Location textLocation = new Location(350, 450);
    private int delayTime = 600;
    private Hand[] hands;
    private int currentRound = 0;

    public void setStatus(String string) {
        setStatusText(string);
    }

    private int[] scores = new int[nbPlayers];

    private int[] autoIndexHands = new int[nbPlayers];
    private boolean isAuto = false;
    private Hand playingArea;

    private Card selected;
    private Card drawnCard;
    private boolean isEndingTurn = false;

    // ===== Score UI =====
    private void initScore() {
        for (int i = 0; i < nbPlayers; i++) {
            String text = "[P" + i + ": " + scores[i] + "]";
            scoreActors[i] = new TextActor(text, Color.WHITE, bgColor, bigFont);
            addActor(scoreActors[i], scoreLocations[i]);
        }

        pileNameActors[0] = new TextActor("Computer", Color.WHITE, bgColor, smallFont);
        addActor(pileNameActors[0], pileNameLocations[0]);

        pileNameActors[1] = new TextActor("Human", Color.WHITE, bgColor, smallFont);
        addActor(pileNameActors[1], pileNameLocations[1]);
    }

    private void updateScore(int player) {
        removeActor(scoreActors[player]);
        int displayScore = Math.max(scores[player], 0);
        String text = "P" + player + "[" + displayScore + "]";
        scoreActors[player] = new TextActor(text, Color.WHITE, bgColor, bigFont);
        addActor(scoreActors[player], scoreLocations[player]);
    }

    private void initScores() {
        Arrays.fill(scores, 0);
    }

    // ===== Piles =====
    private void setupPiles() {
        discard = new Hand(deck);
        RowLayout discardLayout = new RowLayout(discardLocation, pileWidth);
        discardLayout.setRotationAngle(270);
        discard.setView(this, discardLayout);
        discard.draw();
        discardNameActor = new TextActor("Discard Pile", Color.WHITE, bgColor, smallFont);
        addActor(discardNameActor, discardNameLocation);

        RowLayout packLayout = new RowLayout(packLocation, pileWidth);
        packLayout.setRotationAngle(90);
        pack.setView(this, packLayout);
        pack.draw();
        packNameActor = new TextActor("Stockpile", Color.WHITE, bgColor, smallFont);
        addActor(packNameActor, packNameLocation);

        discard.addCardListener(new CardAdapter() {
            @Override
            public void leftDoubleClicked(Card card) {
                drawnCard = card;
                discard.remove(drawnCard, true);
                discard.setTouchEnabled(false);
                discard.draw();
            }
        });

        pack.addCardListener(new CardAdapter() {
            @Override
            public void leftDoubleClicked(Card card) {
                drawnCard = card;
                pack.remove(drawnCard, true);
                pack.setTouchEnabled(false);
                pack.draw();
            }
        });
    }

    private void sortHand(Hand hand) {
        List<Card> cards = hand.getCardList();
        Comparator<Card> cardComparator = (o1, o2) -> {
            Suit suit1 = (Suit) o1.getSuit();
            Suit suit2 = (Suit) o2.getSuit();
            Rank rank1 = (Rank) o1.getRank();
            Rank rank2 = (Rank) o2.getRank();

            if (suit1.ordinal() - suit2.ordinal() != 0) {
                return suit1.ordinal() - suit2.ordinal();
            }
            return rank1.getShortHandValue() - rank2.getShortHandValue();
        };
        cards.sort(cardComparator);
    }

    private void initRound() {
        hands = new Hand[nbPlayers];
        for (int i = 0; i < nbPlayers; i++) {
            hands[i] = new Hand(deck);
        }
        dealingOut(hands);

        for (int i = 0; i < nbPlayers; i++) {
            sortHand(hands[i]);
        }
        arrangeStockpile();

        playingArea = new Hand(deck);

        playingArea.setView(this, new RowLayout(playingLocation, (playingArea.getNumberOfCards() + 3) * cardWidth));
        playingArea.draw();

        // Set up human player for interaction
        CardListener cardListener = new CardAdapter()  // Human Player plays card
        {
            public void leftDoubleClicked(Card card) {
                selected = card;
                hands[HUMAN_PLAYER_INDEX].setTouchEnabled(false);
            }
        };
        hands[HUMAN_PLAYER_INDEX].addCardListener(cardListener);

        // graphics
        RowLayout[] layouts = new RowLayout[nbPlayers];
        for (int i = 0; i < nbPlayers; i++) {
            layouts[i] = new RowLayout(handLocations[i], handWidth);
            layouts[i].setRotationAngle(i);
            hands[i].setView(this, layouts[i]);
            hands[i].setTargetArea(new TargetArea(playingLocation));
            hands[i].draw();
        }

        setupPiles();
    }

    private void setupButtons() {
        addActor(endTurnActor, endTurnLocation);
        endTurnActor.addButtonListener(new GGButtonListener() {
            @Override public void buttonPressed(GGButton ggButton) { isEndingTurn = true; }
            @Override public void buttonReleased(GGButton ggButton) { }
            @Override public void buttonClicked(GGButton ggButton) { }
        });

        if (isGinMode) {
            // Gin Button
            addActor(ginActor, ginLocation);
            ginActor.addButtonListener(new GGButtonListener() {
                @Override public void buttonPressed(GGButton ggButton) {
                    isGinDeclared = true;
                    ginDeclarer   = HUMAN_PLAYER_INDEX;
                    isEndingTurn  = true;
                }
                @Override public void buttonReleased(GGButton ggButton) { }
                @Override public void buttonClicked(GGButton ggButton) { }
            });

            // Knock Button
            addActor(knockActor, knockLocation);
            knockActor.addButtonListener(new GGButtonListener() {
                @Override public void buttonPressed(GGButton ggButton) {
                    isKnockDeclared = true;
                    knocker         = HUMAN_PLAYER_INDEX;
                    isEndingTurn    = true;
                }
                @Override public void buttonReleased(GGButton ggButton) { }
                @Override public void buttonClicked(GGButton ggButton) { }
            });

            // Hide Rummy button in Gin mode
            rummyActor.setMouseTouchEnabled(false);
            rummyActor.hide();

        } else {
            // Classic Rummy button
            addActor(rummyActor, rummyLocation);
            rummyActor.addButtonListener(new GGButtonListener() {
                @Override public void buttonPressed(GGButton ggButton) {
                    isRummyDeclared = true;
                    rummyDeclarer   = HUMAN_PLAYER_INDEX;
                    isEndingTurn    = true;
                }
                @Override public void buttonReleased(GGButton ggButton) { }
                @Override public void buttonClicked(GGButton ggButton) { }
            });

            // Hide Gin/Knock in classic mode
            ginActor.hide();
            knockActor.hide();
        }
    }

    /** Enable/disable during the human decision window */
    private void enableRummyButton(boolean enable) {
        if (!isGinMode && rummyActor != null) {
            rummyActor.setMouseTouchEnabled(enable);
        }
    }

    private void enableGinButtons(boolean enable) {
        if (isGinMode) {
            if (ginActor   != null) ginActor.setMouseTouchEnabled(enable);
            if (knockActor != null) knockActor.setMouseTouchEnabled(enable);
        }
    }

    // ===== Helpers =====
    public static Card randomCard(ArrayList<Card> list) {
        int x = random.nextInt(list.size());
        return list.get(x);
    }

    private String getCardName(Card card) {
        Suit suit = (Suit) card.getSuit();
        Rank rank = (Rank) card.getRank();
        return rank.getShortHandValue() + suit.getSuitShortHand();
    }

    private Rank getRankFromString(String cardName) {
        String rankString = cardName.substring(0, cardName.length() - 1);
        Integer rankValue = Integer.parseInt(rankString);

        for (Rank rank : Rank.values()) {
            if (rank.getShortHandValue() == rankValue) {
                return rank;
            }
        }

        return Rank.ACE;
    }

    private Suit getSuitFromString(String cardName) {
        String rankString = cardName.substring(0, cardName.length() - 1);
        String suitString = cardName.substring(cardName.length() - 1, cardName.length());
        Integer rankValue = Integer.parseInt(rankString);

        for (Suit suit : Suit.values()) {
            if (suit.getSuitShortHand().equals(suitString)) {
                return suit;
            }
        }
        return Suit.CLUBS;
    }


    private Card getCardFromList(List<Card> cards, String cardName) {
        Rank existingRank = getRankFromString(cardName);
        Suit existingSuit = getSuitFromString(cardName);
        for (Card card : cards) {
            Suit suit = (Suit) card.getSuit();
            Rank rank = (Rank) card.getRank();
            if (suit.getSuitShortHand().equals(existingSuit.getSuitShortHand())
                    && rank.getShortHandValue() == existingRank.getShortHandValue()) {
                return card;
            }
        }

        return null;
    }

    private void arrangeStockpile() {
        String roundString = "rounds." + currentRound;
        String stockpileKey = roundString + ".stockpile.cards";
        pack.shuffle(false);
        String topCardsValue = properties.getProperty(stockpileKey);
        if (topCardsValue == null) {
            return;
        }
        String[] topCards = topCardsValue.split(",");
        for (int i = topCards.length - 1; i >= 0; i--) {
            String topCard = topCards[i];
            if (topCard.length() <= 1) {
                continue;
            }
            Card card = getCardFromList(pack.getCardList(), topCard);
            List<Card> cardList = pack.getCardList();
            if (card != null) {
                cardList.remove(card);
                cardList.add(card);
            }
        }
    }

    private void dealingOut(Hand[] hands) {
        pack = deck.toHand(false);
        String roundString = "rounds." + currentRound;
        for (int i = 0; i < nbPlayers; i++) {
            String initialCardsKey = roundString + ".players." + i + ".initialcards";
            String initialCardsValue = properties.getProperty(initialCardsKey);
            if (initialCardsValue == null) {
                continue;
            }
            String[] initialCards = initialCardsValue.split(",");
            for (String initialCard : initialCards) {
                if (initialCard.length() <= 1) {
                    continue;
                }
                Card card = getCardFromList(pack.getCardList(), initialCard);
                if (card != null) {
                    card.removeFromHand(false);
                    hands[i].insert(card, false);
                }
            }
        }

        for (int i = 0; i < nbPlayers; i++) {
            int cardsToDealt = nbStartCards - hands[i].getNumberOfCards();
            for (int j = 0; j < cardsToDealt; j++) {
                if (pack.isEmpty()) return;
                Card dealt = randomCard(pack.getCardList());
                dealt.removeFromHand(false);
                hands[i].insert(dealt, false);
            }
        }
    }

    private String cardDescriptionForLog(Card card) {
        Rank cardRank = (Rank) card.getRank();
        Suit cardSuit = (Suit) card.getSuit();
        return cardRank.getCardLog() + cardSuit.getSuitShortHand();
    }

    /**
     * Logging Logic
     * @param player
     * @param discardCard
     * @param pickupCard
     */

    private void addCardPlayedToLog(int player, Card discardCard, Card pickupCard, String action) {
        logResult.append("P" + player + "-");
        logResult.append(cardDescriptionForLog(pickupCard) + "-");
        logResult.append(cardDescriptionForLog(discardCard));
        if (action != null) {
            logResult.append("-" + action);
        }

        logResult.append(",");
    }

    private void addRoundInfoToLog(int roundNumber) {
        logResult.append("\n");
        logResult.append("Round" + roundNumber + ":");
    }

    private void addTurnInfoToLog(int turnNumber) {
        logResult.append("\n");
        logResult.append("Turn" + turnNumber + ":");
    }

    private void addPlayerCardsToLog() {
        logResult.append("\n");
        logResult.append("Initial Cards:");
        for (int i = 0; i < nbPlayers; i++) {
            logResult.append("P" + i + "-");
            logResult.append(convertCardListoString(hands[i]));
        }
    }

    private String convertCardListoString(Hand hand) {
        StringBuilder sb = new StringBuilder();
        sb.append(hand.getCardList().stream().map(card -> {
            Rank rank = (Rank) card.getRank();
            Suit suit = (Suit) card.getSuit();
            return rank.getCardLog() + suit.getSuitShortHand();
        }).collect(Collectors.joining(",")));
        sb.append("-");
        return sb.toString();
    }

    private void addEndOfRoundToLog() {
        logResult.append("\n");
        logResult.append("Round" + currentRound +  " End:P0-" + scores[0] + ",P1-" + scores[1]);
    }

    private void addEndOfGameToLog(List<Integer> winners) {
        logResult.append("\n");
        if (winners.size() == 1) {
            logResult.append("Game End:P" + winners.get(0));
        } else {
            // Multiple winners (draw)
            logResult.append("Game End:");
            for (int i = 0; i < winners.size(); i++) {
                logResult.append("P" + winners.get(i));
                if (i < winners.size() - 1) {
                    logResult.append(",");
                }
            }
        }
    }

    private Card dealTopCard(Hand hand) {
        return hand.getCardList().get(hand.getCardList().size() - 1);
    }

    private Card processTopCardFromPile(Hand pile, Hand hand) {
        delay(thinkingTime);
        Card card = dealTopCard(pile);
        pile.remove(card, false);
        pile.draw();
        hand.insert(card, false);
        sortHand(hand);
        hand.draw();
        return card;
    }

    private void discardCardFromHand(Card card, Hand hand) {
        card.removeFromHand(false);
        discard.insert(card, false);
        discard.draw();
        hand.draw();
    }

    private void waitingForHumanToSelectCard(Hand hand) {
        hand.setTouchEnabled(true);

        selected = null;
        while (null == selected) delay(delayTime);
        hand.setTouchEnabled(false);
    }

    private void drawCardToHand(Hand hand) {
        hand.insert(drawnCard, false);
        sortHand(hand);
        hand.draw();
    }

    private void setTouchEnableIfNotNull(Hand hand, boolean isEnabled) {
        if (hand != null) {
            hand.setTouchEnabled(isEnabled);
        }
    }

    private void waitingForHumanToSelectPile(Hand pile1, Hand pile2) {
        setTouchEnableIfNotNull(pile1, true);
        setTouchEnableIfNotNull(pile2, true);
        drawnCard = null;
        while (null == drawnCard) delay(delayTime);
    }

    private void waitingForHumanToEndTurn() {
        endTurnActor.setMouseTouchEnabled(true);
        isEndingTurn = false;

        while (!isEndingTurn && !isRummyDeclared) {
            delay(delayTime);
        }

        endTurnActor.setMouseTouchEnabled(false);
    }

    public Card getRandomCard(Hand hand) {
        delay(thinkingTime);

        int x = random.nextInt(hand.getCardList().size());
        return hand.getCardList().get(x);
    }

    private void processNonAutoPlaying(int nextPlayer, Hand hand) {
        if (HUMAN_PLAYER_INDEX == nextPlayer) {
            isRummyDeclared = false;
            isGinDeclared = false;
            isKnockDeclared = false;

            if (!discard.isEmpty()) {
                setStatus("Player " + nextPlayer + " is playing. Double click a pile to draw");
                waitingForHumanToSelectPile(pack, discard);
            } else {
                setStatus("Player " + nextPlayer + " is playing first. Double click the stockpile to draw");
                waitingForHumanToSelectPile(pack, null);
            }
            drawCardToHand(hand);
            setStatus("Player " + nextPlayer + ": double click a card in hand to discard");

            waitingForHumanToSelectCard(hand);
            discardCardFromHand(selected, hand);

            // Decision window
            if (isGinMode) {
                setStatus("Click End Turn, or declare Gin/Knock");
                enableGinButtons(true);
                waitingForHumanToEndTurn();
                enableGinButtons(false);
            } else {
                setStatus("Click End Turn or declare Rummy");
                enableRummyButton(true);
                waitingForHumanToEndTurn();
                enableRummyButton(false);
            }

            // Log human action
            String action = null;
            if (isGinMode) {
                if (isGinDeclared) action = "GIN";
                else if (isKnockDeclared) action = "KNOCK";
            } else if (isRummyDeclared) {
                action = "RUMMY";
            }
            addCardPlayedToLog(nextPlayer, selected, drawnCard, action);

        } else {
            // Computer
            setStatusText("Player " + nextPlayer + " thinking...");
            if (!discard.isEmpty()) {
                boolean isPickingDiscard = new Random().nextBoolean();
                if (isPickingDiscard) {
                    setStatusText("Player " + nextPlayer + " picks from discard...");
                    drawnCard = processTopCardFromPile(discard, hand);
                } else {
                    setStatusText("Player " + nextPlayer + " picks from stockpile...");
                    drawnCard = processTopCardFromPile(pack, hands[nextPlayer]);
                }
            } else {
                setStatusText("Player " + nextPlayer + " picks from stockpile...");
                drawnCard = processTopCardFromPile(pack, hands[nextPlayer]);
            }
            selected = getRandomCard(hands[nextPlayer]);
            discardCardFromHand(selected, hand);

            if (isGinMode) {
                // Try Gin
                if (MeldDetector.allCardsFormedIntoMelds(hand)) {
                    isGinDeclared = true;
                    ginDeclarer = nextPlayer;
                    addCardPlayedToLog(nextPlayer, selected, drawnCard, "GIN");
                } else {
                    // Try Knock (deadwood <= threshold)
                    MeldDetector.MeldAnalysis a = MeldDetector.findBestMelds(hand);
                    if (a.getDeadwoodValue() <= knockThreshold) {
                        isKnockDeclared = true;
                        knocker = nextPlayer;
                        addCardPlayedToLog(nextPlayer, selected, drawnCard, "KNOCK");
                    } else {
                        addCardPlayedToLog(nextPlayer, selected, drawnCard, null);
                    }
                }
            } else {
                // Classic: Rummy if all melds (13)
                if (MeldDetector.allCardsFormedIntoMelds(hand)) {
                    isRummyDeclared = true;
                    rummyDeclarer = nextPlayer;
                    addCardPlayedToLog(nextPlayer, selected, drawnCard, "RUMMY");
                } else {
                    addCardPlayedToLog(nextPlayer, selected, drawnCard, null);
                }
            }
        }
    }

    private List<CardAction> getActionFromAutoMovement(String nextMovement) {
        List<CardAction> actions = new ArrayList<>();
        String[] movementComponents = nextMovement.split("-");
        switch (movementComponents.length) {
            case 1:
                actions.add(CardAction.NONE);
                break;
            case 2:
                actions.add(CardAction.valueOf(movementComponents[0]));
                break;
            case 3:
                actions.add(CardAction.valueOf(movementComponents[0]));
                actions.add(CardAction.valueOf(movementComponents[2]));
                break;
        }
        return actions;
    }

    private Card getCardElementFromAutoMovement(Hand hand, String nextMovement) {
        String[] movementComponents = nextMovement.split("-");
        switch (movementComponents.length) {
            case 1:
                return getCardFromList(hand.getCardList(), movementComponents[0]);
            case 2:
                return getCardFromList(hand.getCardList(), movementComponents[1]);
            case 3:
                return getCardFromList(hand.getCardList(), movementComponents[1]);
        }
        return null;
    }

    private boolean playARound() {
        int nextPlayer = roundWinner;
        addRoundInfoToLog(currentRound);
        addPlayerCardsToLog();
        int i = 0;
        boolean isContinue = true;
        setupPlayerAutoMovements();
        while (isContinue) {
            i++;
            addTurnInfoToLog(i);
            for (int j = 0; j < nbPlayers; j++) {
                Hand hand = hands[nextPlayer];

                if (isAuto) {
                    int nextPlayerAutoIndex = autoIndexHands[nextPlayer];
                    List<String> nextPlayerMovement = playerAutoMovements.get(nextPlayer);
                    String nextMovement = "";
                    boolean hasRunAuto = false;
                    if (nextPlayerMovement.size() > nextPlayerAutoIndex) {
                        nextMovement = nextPlayerMovement.get(nextPlayerAutoIndex);
                        if (!nextMovement.isEmpty()) {
                            hasRunAuto = true;
                            nextPlayerAutoIndex++;

                            autoIndexHands[nextPlayer] = nextPlayerAutoIndex;
                            setStatus("Player " + nextPlayer + " is playing");
                            List<CardAction> cardActions = getActionFromAutoMovement(nextMovement);
                            CardAction cardAction = cardActions.get(0);
                            Card card = null;
                            if (cardAction == CardAction.DISCARD) {
                                card = processTopCardFromPile(discard, hand);
                                selected = getCardElementFromAutoMovement(hand, nextMovement);
                                discardCardFromHand(selected, hand);
                            } else if (cardAction == CardAction.STOCKPILE) {
                                card = processTopCardFromPile(pack, hand);
                                selected = getCardElementFromAutoMovement(hand, nextMovement);
                                discardCardFromHand(selected, hand);
                            }

                            delay(thinkingTime);
                            if (cardActions.size() > 1) {
                                CardAction lastAction = cardActions.get(1);
                                if (lastAction == CardAction.RUMMY) {
                                    setStatus("Player " + nextPlayer + " is declaring rummy...");
                                    isRummyDeclared = true;
                                    rummyDeclarer = nextPlayer;
                                    isContinue = false;
                                    addCardPlayedToLog(nextPlayer, selected, card, lastAction.name());
                                    break;
                                } else if (lastAction == CardAction.GIN) {
                                    setStatus("Player " + nextPlayer + " is declaring gin...");
                                    isGinDeclared = true;
                                    ginDeclarer = nextPlayer;
                                    isContinue = false;
                                    addCardPlayedToLog(nextPlayer, selected, card, "GIN");
                                    break;
                                } else if (lastAction == CardAction.KNOCK) {
                                    setStatus("Player " + nextPlayer + " is declaring knock...");
                                    isKnockDeclared = true;
                                    knocker = nextPlayer;
                                    isContinue = false;
                                    addCardPlayedToLog(nextPlayer, selected, card, "KNOCK");
                                    break;
                                }
                            } else {
                                addCardPlayedToLog(nextPlayer, selected, card, null);
                            }
                            delay(delayTime);
                        }
                    }

                    if (!hasRunAuto) {
                        processNonAutoPlaying(nextPlayer, hand);
                    }
                }

                if (!isAuto) {
                    processNonAutoPlaying(nextPlayer, hand);
                }

                // ===== Declarations handling (verbose + consistent) =====
                if (!isGinMode) {
                    // ----- Classic Rummy -----
                    if (isRummyDeclared) {
                        System.out.println("\n>>> VALIDATING RUMMY DECLARATION <<<");
                        System.out.println("Declarer: Player " + rummyDeclarer);
                        Hand declarerHand = hands[rummyDeclarer];
                        System.out.println("Cards in hand: " + declarerHand.getNumberOfCards());

                        boolean allMelded = MeldDetector.allCardsFormedIntoMelds(declarerHand);
                        System.out.println("All cards form melds? " + allMelded);

                        if (!allMelded) {
                            System.out.println("INVALID RUMMY - continuing game");
                            setStatus("Invalid Rummy declaration - not all cards form melds");
                            isRummyDeclared = false;
                            rummyDeclarer = -1;
                            // continue playing
                        } else {
                            System.out.println("VALID RUMMY - ending round");
                            isContinue = false;
                            break;
                        }
                    }
                } else {
                    // ----- Gin Rummy -----
                    if (isGinDeclared) {
                        System.out.println("\n>>> VALIDATING GIN DECLARATION <<<");
                        System.out.println("Declarer: Player " + ginDeclarer);
                        Hand declarerHand = hands[ginDeclarer];
                        System.out.println("Cards in hand: " + declarerHand.getNumberOfCards());

                        boolean allMelded = MeldDetector.allCardsFormedIntoMelds(declarerHand);
                        System.out.println("All cards form melds? " + allMelded);

                        if (!allMelded) {
                            System.out.println("INVALID GIN - continuing game");
                            setStatus("Invalid Gin declaration - not all cards form melds");
                            isGinDeclared = false;
                            ginDeclarer = -1;
                            // continue playing
                        } else {
                            System.out.println("VALID GIN - ending round");
                            isContinue = false;
                            break;
                        }
                    }

                    if (isKnockDeclared) {
                        System.out.println("\n>>> KNOCK DECLARED <<<");
                        System.out.println("Knocker: Player " + knocker);
                        // Helpful debug: show current deadwood of knocker at declaration time
                        MeldDetector.MeldAnalysis knockAnalysis = MeldDetector.findBestMelds(hands[knocker]);
                        System.out.println("Deadwood value at knock: " + knockAnalysis.getDeadwoodValue());
                        setStatus("Knock declared");
                        // Knock ends the round immediately; scoring handles the difference.
                        isContinue = false;
                        break;
                    }
                }

                if (pack.isEmpty()) {
                    setStatus("Stockpile is exhausted. Calculating players' scores now.");
                    isContinue = false;
                }

                nextPlayer = (nextPlayer + 1) % nbPlayers;
            }
        }
        // Calculate scores
        calculateRoundScores();

        addEndOfRoundToLog();
        return false;
    }

    private void setupPlayerAutoMovements() {
        String roundString = "rounds." + currentRound;
        String player0AutoMovement = properties.getProperty(roundString + ".players.0.cardsPlayed");
        String player1AutoMovement = properties.getProperty(roundString + ".players.1.cardsPlayed");

        String[] playerMovements = new String[]{"", ""};
        if (player0AutoMovement != null) {
            playerMovements[0] = player0AutoMovement;
        }

        if (player1AutoMovement != null) {
            playerMovements[1] = player1AutoMovement;
        }

        for (int i = 0; i < playerMovements.length; i++) {
            String movementString = playerMovements[i];
            List<String> movements = Arrays.asList(movementString.split(","));
            playerAutoMovements.add(movements);
        }
    }

    public String runApp() {
        setTitle("Pinochle  (V" + version + ") Constructed for UofM SWEN30006 with JGameGrid (www.aplu.ch)");
        setStatusText("Initializing...");
        initScores();
        initScore();
        setupButtons();

        currentRound = 0;
        boolean isContinue = true;
        int winningScore = 100;

        while(isContinue) {
            initRound();
            playARound();

            // Check if anyone reached 100 points
            for (int i = 0; i < nbPlayers; i++) {
                updateScore(i);
                if (scores[i] >= winningScore) {
                    isContinue = false;
                    break;
                }
            }

            currentRound++;
        }

        for (int i = 0; i < nbPlayers; i++) updateScore(i);
        int maxScore = 0;
        for (int i = 0; i < nbPlayers; i++) if (scores[i] > maxScore) maxScore = scores[i];
        List<Integer> winners = new ArrayList<Integer>();
        for (int i = 0; i < nbPlayers; i++) if (scores[i] == maxScore) winners.add(i);
        String winText;
        if (winners.size() == 1) {
            winText = "Game over. Winner is player: " +
                    winners.iterator().next();
        } else {
            winText = "Game Over. Drawn winners are players: " +
                    String.join(", ", winners.stream().map(String::valueOf).collect(Collectors.toList()));
        }
        addActor(new Actor("sprites/gameover.gif"), textLocation);
        setStatusText(winText);
        refresh();
        addEndOfGameToLog(winners);

        return logResult.toString();
    }

    public Rummy(Properties properties) {
        super(700, 700, 30);
        this.properties = properties;
        isAuto = Boolean.parseBoolean(properties.getProperty("isAuto"));
        thinkingTime = Integer.parseInt(properties.getProperty("thinkingTime", "200"));
        delayTime = Integer.parseInt(properties.getProperty("delayTime", "50"));

        // Mode selection (default classic)
        String mode = properties.getProperty("mode", "classic").toLowerCase();
        isGinMode = mode.equals("gin");

        if (isGinMode) {
            nbStartCards = 10; // Gin uses 10 cards regardless of number_cards prop
        } else {
            nbStartCards = Integer.parseInt(properties.getProperty("number_cards", "13"));
        }

        // Knock threshold (spec example uses 7)
        knockThreshold = Integer.parseInt(properties.getProperty("knock_threshold", "7"));
    }

    private void calculateRoundScores() {
        System.out.println("\n========== CALCULATING ROUND SCORES ==========");
        System.out.println("mode=" + (isGinMode ? "GIN" : "CLASSIC")
                + " | Rummy=" + isRummyDeclared + " rDec=" + rummyDeclarer
                + " | Gin=" + isGinDeclared + " gDec=" + ginDeclarer
                + " | Knock=" + isKnockDeclared + " kDec=" + knocker);

        MeldDetector.MeldAnalysis[] analyses = new MeldDetector.MeldAnalysis[nbPlayers];
        for (int i = 0; i < nbPlayers; i++) {
            analyses[i] = MeldDetector.findBestMelds(hands[i]);
            System.out.println("P" + i + " melds=" + analyses[i].getMelds().size()
                    + " deadwood=" + analyses[i].getDeadwoodValue());
        }

        if (isGinMode) {
            if (isGinDeclared && ginDeclarer != -1) {
                int opp = (ginDeclarer + 1) % nbPlayers;
                int pts = analyses[opp].getDeadwoodValue();
                scores[ginDeclarer] += pts;
                roundWinner = ginDeclarer;
                setStatus("Player " + ginDeclarer + " wins with GIN! +" + pts + " points");
            } else if (isKnockDeclared && knocker != -1) {
                int d0 = analyses[0].getDeadwoodValue();
                int d1 = analyses[1].getDeadwoodValue();
                int opp = (knocker + 1) % nbPlayers;
                int kd = (knocker == 0) ? d0 : d1;
                int od = (opp == 0) ? d0 : d1;

                if (kd < od) {
                    int diff = od - kd;
                    scores[knocker] += diff;
                    roundWinner = knocker;
                    setStatus("Knock success by P" + knocker + " +" + diff + " points");
                } else if (kd > od) {
                    int diff = kd - od;
                    scores[opp] += diff;
                    roundWinner = opp;
                    setStatus("Knock undercut by P" + opp + " +" + diff + " points");
                } else {
                    setStatus("Knock tie - no points");
                }
            } else {
                // Stock exhausted (Gin mode): lower deadwood wins opponent’s total
                int d0 = analyses[0].getDeadwoodValue();
                int d1 = analyses[1].getDeadwoodValue();
                if (d0 < d1) { scores[0] += d1; roundWinner = 0; setStatus("Stock exhausted: P0 wins +" + d1); }
                else if (d1 < d0) { scores[1] += d0; roundWinner = 1; setStatus("Stock exhausted: P1 wins +" + d0); }
                else { setStatus("Stock exhausted: tie - no points"); }
            }
        } else {
            // Classic mode scoring (unchanged)
            if (isRummyDeclared && rummyDeclarer != -1) {
                int opponent = (rummyDeclarer + 1) % nbPlayers;
                int pointsEarned = analyses[opponent].getDeadwoodValue();
                scores[rummyDeclarer] += pointsEarned;
                roundWinner = rummyDeclarer;
                setStatus("Player " + rummyDeclarer + " wins with Rummy! +" + pointsEarned + " points");
            } else {
                int d0 = analyses[0].getDeadwoodValue();
                int d1 = analyses[1].getDeadwoodValue();
                if (d0 < d1) { scores[0] += d1; roundWinner = 0; setStatus("Stock exhausted: P0 wins +" + d1); }
                else if (d1 < d0) { scores[1] += d0; roundWinner = 1; setStatus("Stock exhausted: P1 wins +" + d0); }
                else { setStatus("Stock exhausted: tie - no points"); }
            }
        }

        System.out.println("Final scores: P0=" + scores[0] + ", P1=" + scores[1]);
        System.out.println("==============================================\n");

        // Reset flags for next round
        isRummyDeclared = false; rummyDeclarer = -1;
        isGinDeclared = false;  ginDeclarer = -1;
        isKnockDeclared = false; knocker = -1;
    }
}