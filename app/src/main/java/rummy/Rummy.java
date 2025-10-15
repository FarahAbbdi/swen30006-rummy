package rummy;

import ch.aplu.jcardgame.*;
import ch.aplu.jgamegrid.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

// first commit by dimitri

@SuppressWarnings("serial")
public class Rummy extends CardGame {
    private boolean isRummyDeclared = false;
    private int rummyDeclarer = -1;
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

    private final GGButton rummyActor = new GGButton("sprites/rummy.gif", false);
    private final Location rummyLocation = new Location(80, 650);
    private final GGButton endTurnActor = new GGButton("sprites/end.gif", false);
    private final Location endTurnLocation = new Location(80, 610);
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

    /**
     * Score Section
     */

    private void initScore() {
        for (int i = 0; i < nbPlayers; i++) {
            // scores[i] = 0;
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
        String text = "P" + player + "[" + String.valueOf(displayScore) + "]";
        scoreActors[player] = new TextActor(text, Color.WHITE, bgColor, bigFont);
        addActor(scoreActors[player], scoreLocations[player]);
    }

    private void initScores() {
        Arrays.fill(scores, 0);
    }

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
            @Override
            public void buttonPressed(GGButton ggButton) {
                isEndingTurn = true;
            }

            @Override
            public void buttonReleased(GGButton ggButton) {

            }

            @Override
            public void buttonClicked(GGButton ggButton) {

            }
        });
        addActor(rummyActor, rummyLocation);
        rummyActor.addButtonListener(new GGButtonListener() {
            @Override
            public void buttonPressed(GGButton ggButton) {
                System.out.println("rummy button pressed");
                isRummyDeclared = true;
                rummyDeclarer = HUMAN_PLAYER_INDEX;
                isEndingTurn = true;
            }

            @Override
            public void buttonReleased(GGButton ggButton) {
            }

            @Override
            public void buttonClicked(GGButton ggButton) {
            }
        });
    }

    // return random Card from ArrayList
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
        System.out.println("DISCARD DEBUG: Removing " + cardDescriptionForLog(card));
        System.out.println("  Hand size before: " + hand.getNumberOfCards());

        // Use hand.remove() instead of card.removeFromHand()
        boolean removed = hand.remove(card, false);
        System.out.println("  Successfully removed: " + removed);
        System.out.println("  Hand size after removal: " + hand.getNumberOfCards());

        discard.insert(card, false);
        discard.draw();
        hand.draw();

        System.out.println("  Final hand size: " + hand.getNumberOfCards());
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

            if (!discard.isEmpty()) {
                setStatus("Player " + nextPlayer + " is playing. Please double click on a pile to draw");
                waitingForHumanToSelectPile(pack, discard);
            } else {
                setStatus("Player " + nextPlayer + " is playing first. Please double click on the stockpile to draw");
                waitingForHumanToSelectPile(pack, null);
            }
            drawCardToHand(hand);
            setStatus("Player " + nextPlayer + " is playing. Please double click on a card in hand to select");

            waitingForHumanToSelectCard(hand);
            discardCardFromHand(selected, hand);

            setStatus("Player " + nextPlayer + " is playing. Please click on end turn of declare rummy/gin/knock");
            enableRummyButton(true);
            waitingForHumanToEndTurn();
            enableRummyButton(false);
            addCardPlayedToLog(nextPlayer, selected, drawnCard, null);

            if (isRummyDeclared) {
                addCardPlayedToLog(nextPlayer, selected, drawnCard, "RUMMY");
            } else {
                addCardPlayedToLog(nextPlayer, selected, drawnCard, null);
            }
        } else {
            // SMART COMPUTER PLAYER LOGIC WITH DEBUGGING
            System.out.println("\n=== P0 SMART COMPUTER TURN START ===");
            System.out.println("Initial hand (" + hand.getNumberOfCards() + " cards):");
            for (Card c : hand.getCardList()) {
                System.out.println("  " + cardDescriptionForLog(c));
            }

            // Check if computer_smart property is enabled
            boolean isSmartEnabled = Boolean.parseBoolean(properties.getProperty("computer_smart", "false"));
            System.out.println("Computer smart enabled: " + isSmartEnabled);

            if (!isSmartEnabled) {
                System.out.println("Using random logic instead of smart logic");
                // Fall back to original random logic
                if (!discard.isEmpty()) {
                    boolean isPickingDiscard = new Random().nextBoolean();
                    if (isPickingDiscard) {
                        setStatusText("Player " + nextPlayer + " is picking a card from discard pile...");
                        drawnCard = processTopCardFromPile(discard, hand);
                    } else {
                        setStatusText("Player " + nextPlayer + " is picking a card from stockpile...");
                        drawnCard = processTopCardFromPile(pack, hands[nextPlayer]);
                    }
                } else {
                    setStatusText("Player " + nextPlayer + " is picking a card from stockpile...");
                    drawnCard = processTopCardFromPile(pack, hands[nextPlayer]);
                }
                selected = getRandomCard(hands[nextPlayer]);
                discardCardFromHand(selected, hand);

                // Check if computer can declare Rummy
                if (MeldDetector.allCardsFormedIntoMelds(hand)) {
                    isRummyDeclared = true;
                    rummyDeclarer = nextPlayer;
                    addCardPlayedToLog(nextPlayer, selected, drawnCard, "RUMMY");
                } else {
                    addCardPlayedToLog(nextPlayer, selected, drawnCard, null);
                }
                return;
            }

            setStatusText("Player " + nextPlayer + " thinking...");

            Card cardToKeep = null;
            boolean keptCard = false;

            // Step 1: Evaluate discard pile top card
            if (!discard.isEmpty()) {
                Card discardTop = dealTopCard(discard);
                System.out.println("Discard pile top card: " + cardDescriptionForLog(discardTop));

                SmartComputerPlayer.EvaluationResult discardEval =
                        SmartComputerPlayer.evaluateCard(discardTop, hand, deck);

                System.out.println("Discard evaluation satisfies criteria: " + discardEval.satisfiesAnyCriterion());
                System.out.println("  Criterion 1 (immediate meld): " + discardEval.criterion1);
                System.out.println("  Criterion 2 (rank gap): " + discardEval.criterion2);
                System.out.println("  Criterion 3 (max suit): " + discardEval.criterion3);
                System.out.println("  Criterion 4 (deadwood rank): " + discardEval.criterion4);

                if (discardEval.satisfiesAnyCriterion()) {
                    setStatusText("Player " + nextPlayer + " is picking from discard pile...");
                    drawnCard = processTopCardFromPile(discard, hand);
                    cardToKeep = drawnCard;
                    keptCard = true;
                    System.out.println("P0 PICKED UP from discard: " + cardDescriptionForLog(drawnCard));
                } else {
                    System.out.println("P0 REJECTED discard pile card");
                }
            } else {
                System.out.println("Discard pile is empty");
            }

            // Step 2: If didn't take discard, try stockpile
            if (!keptCard) {
                setStatusText("Player " + nextPlayer + " is picking from stockpile...");
                Card stockpileCard = processTopCardFromPile(pack, hand);
                drawnCard = stockpileCard;
                System.out.println("P0 drew from stockpile: " + cardDescriptionForLog(drawnCard));

                SmartComputerPlayer.EvaluationResult stockpileEval =
                        SmartComputerPlayer.evaluateCard(stockpileCard, hand, deck);

                System.out.println("Stockpile evaluation satisfies criteria: " + stockpileEval.satisfiesAnyCriterion());
                System.out.println("  Criterion 1 (immediate meld): " + stockpileEval.criterion1);
                System.out.println("  Criterion 2 (rank gap): " + stockpileEval.criterion2);
                System.out.println("  Criterion 3 (max suit): " + stockpileEval.criterion3);
                System.out.println("  Criterion 4 (deadwood rank): " + stockpileEval.criterion4);

                if (stockpileEval.satisfiesAnyCriterion()) {
                    cardToKeep = stockpileCard;
                    keptCard = true;
                    System.out.println("P0 KEEPING stockpile card");
                } else {
                    System.out.println("P0 will DISCARD stockpile card");
                }
            }

            // DEBUG: Print hand after drawing
            System.out.println("Hand after drawing (" + hand.getNumberOfCards() + " cards):");
            for (Card c : hand.getCardList()) {
                System.out.println("  " + cardDescriptionForLog(c));
            }

            // Step 3: Select card to discard (always required to get back to 13 cards)
            if (keptCard) {
                selected = SmartComputerPlayer.selectCardToDiscard(hand, deck);
                System.out.println("Smart discard selection: " + cardDescriptionForLog(selected));
            } else {
                // Discard the drawn card
                selected = drawnCard;
                System.out.println("Discarding drawn card: " + cardDescriptionForLog(selected));
            }

            discardCardFromHand(selected, hand);

            // DEBUG: Print hand after discarding
            System.out.println("Hand after discarding (" + hand.getNumberOfCards() + " cards):");
            for (Card c : hand.getCardList()) {
                System.out.println("  " + cardDescriptionForLog(c));
            }

            // Step 4: Check if we can declare RUMMY (now with exactly 13 cards)
            System.out.println("Checking for RUMMY declaration...");
            boolean canDeclareRummy = MeldDetector.allCardsFormedIntoMelds(hand);
            System.out.println("Can declare RUMMY? " + canDeclareRummy);

            if (canDeclareRummy) {
                setStatusText("Player " + nextPlayer + " is declaring rummy...");
                isRummyDeclared = true;
                rummyDeclarer = nextPlayer;
                System.out.println("P0 DECLARING RUMMY!");
                addCardPlayedToLog(nextPlayer, selected, drawnCard, null);
            } else {
                System.out.println("P0 NOT declaring RUMMY - continuing game");
                addCardPlayedToLog(nextPlayer, selected, drawnCard, null);
            }

            System.out.println("=== P0 SMART COMPUTER TURN END ===\n");
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
                                    setStatus("Player " + nextPlayer + " is ginning...");
                                    addCardPlayedToLog(nextPlayer, selected, card, lastAction.name());
                                    isContinue = false;
                                    break;
                                } else if (lastAction == CardAction.KNOCK) {
                                    setStatus("Player " + nextPlayer + " is knocking...");
                                    addCardPlayedToLog(nextPlayer, selected, card, lastAction.name());
                                    isContinue = false;
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

                if (isRummyDeclared) {
                    System.out.println("\n>>> VALIDATING RUMMY DECLARATION <<<");
                    System.out.println("Declarer: Player " + nextPlayer);
                    Hand declarerHand = hands[nextPlayer];
                    System.out.println("Cards in hand: " + declarerHand.getNumberOfCards());

                    boolean allMelded = MeldDetector.allCardsFormedIntoMelds(declarerHand);
                    System.out.println("All cards form melds? " + allMelded);

                    if (!allMelded) {
                        System.out.println("INVALID RUMMY - continuing game");
                        setStatus("Invalid Rummy declaration - not all cards form melds");
                        isRummyDeclared = false;
                        rummyDeclarer = -1;
                        // Don't break - continue playing
                    } else {
                        System.out.println("VALID RUMMY - ending round");
                        isContinue = false;
                        break; // Break out of for loop
                    }
                }

                if (pack.isEmpty()) {
                    setStatus("Stockpile is exhausted. Calculating players' scores now.");
                    isContinue = false;
                }

                nextPlayer = (nextPlayer + 1) % nbPlayers;
            }
            i++;
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
        nbStartCards = Integer.parseInt(properties.getProperty("number_cards", "13"));
    }

    private void calculateRoundScores() {
        System.out.println("\n========== CALCULATING ROUND SCORES ==========");
        System.out.println("isRummyDeclared: " + isRummyDeclared);
        System.out.println("rummyDeclarer: " + rummyDeclarer);

        // Analyze both players' hands
        MeldDetector.MeldAnalysis[] analyses = new MeldDetector.MeldAnalysis[nbPlayers];

        for (int i = 0; i < nbPlayers; i++) {
            analyses[i] = MeldDetector.findBestMelds(hands[i]);
            System.out.println("Player " + i + " has " + hands[i].getNumberOfCards() + " cards");
            System.out.println("  Melds: " + analyses[i].getMelds().size());
            System.out.println("  Deadwood value: " + analyses[i].getDeadwoodValue());
        }

        // Determine winner and award points
        if (isRummyDeclared && rummyDeclarer != -1) {
            System.out.println("SCENARIO: Rummy declared by Player " + rummyDeclarer);

            // Verify the RUMMY declaration is valid
            Hand declarerHand = hands[rummyDeclarer];
            boolean allMelded = MeldDetector.allCardsFormedIntoMelds(declarerHand);

            if (allMelded) {
                int opponent = (rummyDeclarer + 1) % nbPlayers;
                int pointsEarned = analyses[opponent].getDeadwoodValue();
                scores[rummyDeclarer] += pointsEarned;
                roundWinner = rummyDeclarer;
                System.out.println("Valid RUMMY! Points earned: " + pointsEarned);
                setStatus("Player " + rummyDeclarer + " wins with Rummy! +" + pointsEarned + " points");
            } else {
                System.out.println("Invalid RUMMY declaration!");
                // Handle invalid RUMMY - this shouldn't happen with smart player
            }
        } else {
            // ... rest of stockpile exhausted logic ...
        }

        // Reset flags
        isRummyDeclared = false;
        rummyDeclarer = -1;
    }

    private void enableRummyButton(boolean enable) {
        if (rummyActor != null) {
            rummyActor.setMouseTouchEnabled(enable);
        }
    }
}
