package rummy;

import ch.aplu.jcardgame.*;
import ch.aplu.jgamegrid.*;
import rummy.smartcomputer.SmartComputerPlayer;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;


@SuppressWarnings("serial")
public class Rummy extends CardGame {
    // ===== Strategy Pattern =====
    private GameModeStrategy strategy;

    // ===== Game state (mode-agnostic) =====
    private boolean stockExhaustedThisRound = false;

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

    // Smart Computer Player
    private SmartComputerPlayer smartPlayer;

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
        String text = "P" + player + "[" + String.valueOf(displayScore) + "]";
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
        // --- RESET auto scripting state for the new round ---
        playerAutoMovements.clear();
        Arrays.fill(autoIndexHands, 0);

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
        // Setup End Turn button (mode-agnostic)
        addActor(endTurnActor, endTurnLocation);
        endTurnActor.addButtonListener(new GGButtonListener() {
            @Override public void buttonPressed(GGButton ggButton) { isEndingTurn = true; }
            @Override public void buttonReleased(GGButton ggButton) { }
            @Override public void buttonClicked(GGButton ggButton) { }
        });

        // Setup mode-specific declaration buttons
        setupDeclarationButtons();

        // Let strategy control which buttons are visible
        strategy.setupButtons(Rummy.this);
    }

    /**
     * Setup all declaration buttons (Rummy, Gin, Knock)
     * Strategy will control visibility
     */
    private void setupDeclarationButtons() {
        // Rummy Button (Classic mode)
        addActor(rummyActor, rummyLocation);
        rummyActor.addButtonListener(new GGButtonListener() {
            @Override public void buttonPressed(GGButton ggButton) {
                handleDeclaration("RUMMY", HUMAN_PLAYER_INDEX);
            }
            @Override public void buttonReleased(GGButton ggButton) { }
            @Override public void buttonClicked(GGButton ggButton) { }
        });

        // Gin Button (Gin mode)
        addActor(ginActor, ginLocation);
        ginActor.addButtonListener(new GGButtonListener() {
            @Override public void buttonPressed(GGButton ggButton) {
                handleDeclaration("GIN", HUMAN_PLAYER_INDEX);
            }
            @Override public void buttonReleased(GGButton ggButton) { }
            @Override public void buttonClicked(GGButton ggButton) { }
        });


        // Knock Button (Gin mode)
        addActor(knockActor, knockLocation);
        knockActor.addButtonListener(new GGButtonListener() {
            @Override public void buttonPressed(GGButton ggButton) {
                handleDeclaration("KNOCK", HUMAN_PLAYER_INDEX);
            }
            @Override public void buttonReleased(GGButton ggButton) { }
            @Override public void buttonClicked(GGButton ggButton) { }
        });
    }

    /**
     * Shows/hides the Rummy button (for Classic mode)
     * Called by strategy to control button visibility
     */
    public void showRummyButton(boolean show) {
        if (rummyActor == null) {
            System.out.println("WARNING: rummyActor is null");
            return;
        }

        if (show) {
            rummyActor.show();
            rummyActor.setMouseTouchEnabled(false); // Disabled by default
        } else {
            rummyActor.hide();
            rummyActor.setMouseTouchEnabled(false);
        }
    }

    /**
     * Shows/hides the Gin and Knock buttons (for Gin mode)
     * Called by strategy to control button visibility
     */
    public void showGinButtons(boolean show) {
        if (ginActor == null || knockActor == null) {
            System.out.println("WARNING: ginActor or knockActor is null");
            return;
        }

        if (show) {
            ginActor.show();
            knockActor.show();
            ginActor.setMouseTouchEnabled(false); // Disabled by default
            knockActor.setMouseTouchEnabled(false);
        } else {
            ginActor.hide();
            knockActor.hide();
            ginActor.setMouseTouchEnabled(false);
            knockActor.setMouseTouchEnabled(false);
        }
    }

    /**
     * Unified declaration handler - delegates to strategy
     */
    private void handleDeclaration(String declarationType, int player) {
        boolean isValid = strategy.validateDeclaration(hands[player], player, declarationType);

        if (!isValid) {
            setStatus("Invalid " + declarationType + " declaration");
        }

        // Always end turn after declaration attempt
        isEndingTurn = true;
    }

    /**
     * Enable/disable declaration buttons based on current strategy
     */
    private void enableDeclarationButtons(boolean enable) {
        if (strategy.usesDeclarationType("RUMMY")) {
            rummyActor.setMouseTouchEnabled(enable);
        }

        if (strategy.usesDeclarationType("GIN")) {
            ginActor.setMouseTouchEnabled(enable);
        }

        if (strategy.usesDeclarationType("KNOCK")) {
            knockActor.setMouseTouchEnabled(enable);
        }
    }

    // ===== Helpers =====
    public static Card randomCard(ArrayList<Card> list) {
        int x = random.nextInt(list.size());
        return list.get(x);
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

        while (!isEndingTurn) {
            delay(delayTime);
        }

        endTurnActor.setMouseTouchEnabled(false);
    }

    public Card getRandomCard(Hand hand) {
        delay(thinkingTime);

        int x = random.nextInt(hand.getCardList().size());
        return hand.getCardList().get(x);
    }

    /**
     * Gets the last declaration action that was made (for logging)
     */
    private String getLastDeclarationAction() {
        if (strategy instanceof ClassicRummyStrategy) {
            ClassicRummyStrategy classic = (ClassicRummyStrategy) strategy;
            if (classic.isRummyDeclared()) return "RUMMY";
        }

        if (strategy instanceof GinRummyStrategy) {
            GinRummyStrategy gin = (GinRummyStrategy) strategy;
            if (gin.isGinDeclared()) return "GIN";
            if (gin.isKnockDeclared()) return "KNOCK";
        }

        return null;
    }

    /**
     * Checks if computer player should make a declaration
     * @return declaration type or null if no declaration
     */
    private String checkComputerDeclaration(Hand hand, int player) {
        // Try declarations in priority order based on mode

        if (strategy instanceof GinRummyStrategy) {
            GinRummyStrategy gin = (GinRummyStrategy) strategy;

            // Try Gin first (best outcome)
            if (strategy.canDeclare(hand, "GIN")) {
                strategy.validateDeclaration(hand, player, "GIN");
                return "GIN";
            }

            // Try Knock next
            if (strategy.canDeclare(hand,"KNOCK")) {
                strategy.validateDeclaration(hand, player, "KNOCK");
                return "KNOCK";
            }
        }

        if (strategy instanceof ClassicRummyStrategy) {
            // Try Rummy
            if (strategy.canDeclare(hand, "RUMMY")) {
                strategy.validateDeclaration(hand, player, "RUMMY");
                return "RUMMY";
            }
        }

        return null; // No declaration
    }

    private void processNonAutoPlaying(int nextPlayer, Hand hand) {
        if (HUMAN_PLAYER_INDEX == nextPlayer) {
            // Clear any previous declaration state at start of turn
            // (This is now handled inside strategy, but we ensure clean slate)

            // Card drawing logic (keep as-is)
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

            // Decision window - mode-agnostic
            setStatus("Click End Turn or make a declaration");
            enableDeclarationButtons(true);
            waitingForHumanToEndTurn();
            enableDeclarationButtons(false);

            // Log human action - query strategy for what was declared
            String action = getLastDeclarationAction();
            addCardPlayedToLog(nextPlayer, selected, drawnCard, action);
        } else {
            // COMPUTER PLAYER (P0)
            System.out.println("\n=== P0 COMPUTER TURN START ===");
            System.out.println("Initial hand (" + hand.getNumberOfCards() + " cards):");
            for (Card c : hand.getCardList()) {
                System.out.println("  " + cardDescriptionForLog(c));
            }

            // Check if computer_smart property is enabled
            boolean isSmartEnabled = Boolean.parseBoolean(properties.getProperty("computer_smart", "false"));
            System.out.println("Computer smart enabled: " + isSmartEnabled);

            if (!isSmartEnabled) {
                // ===== RANDOM LOGIC =====
                System.out.println("Using random logic");

                if (!discard.isEmpty()) {
                    boolean isPickingDiscard = new Random().nextBoolean();
                    if (isPickingDiscard) {
                        setStatusText("Player " + nextPlayer + " is picking from discard pile...");
                        drawnCard = processTopCardFromPile(discard, hand);
                    } else {
                        setStatusText("Player " + nextPlayer + " is picking from stockpile...");
                        drawnCard = processTopCardFromPile(pack, hand);
                    }
                } else {
                    setStatusText("Player " + nextPlayer + " is picking from stockpile...");
                    drawnCard = processTopCardFromPile(pack, hand);
                }

                selected = getRandomCard(hand);
                discardCardFromHand(selected, hand);

                // Check for declaration using strategy
                String declaration = checkComputerDeclaration(hand, nextPlayer);
                if (declaration != null) {
                    System.out.println("P0 DECLARING " + declaration + "!");
                    addCardPlayedToLog(nextPlayer, selected, drawnCard, declaration);
                } else {
                    addCardPlayedToLog(nextPlayer, selected, drawnCard, null);
                }

                System.out.println("=== P0 COMPUTER TURN END ===\n");
                return;
            }

            // ===== SMART LOGIC =====
            setStatusText("Player " + nextPlayer + " thinking...");

            boolean keptCard = false;

            // Step 1: Evaluate discard pile
            if (!discard.isEmpty()) {
                Card discardTop = dealTopCard(discard);
                if (smartPlayer.shouldKeepCard(discardTop, hand)) {
                    drawnCard = processTopCardFromPile(discard, hand);
                    keptCard = true;
                }
            }

            // Step 2: If didn't keep discard, try stockpile
            if (!keptCard) {
                drawnCard = processTopCardFromPile(pack, hand);
                if (smartPlayer.shouldKeepCard(drawnCard, hand)) {
                    keptCard = true;
                }
            }

            // Step 3: Select card to discard
            if (keptCard) {
                selected = smartPlayer.selectCardToDiscard(hand, deck);
            } else {
                selected = drawnCard;
            }

            discardCardFromHand(selected, hand);

            // DEBUG: Print hand after discarding
            System.out.println("Hand after discarding (" + hand.getNumberOfCards() + " cards):");
            for (Card c : hand.getCardList()) {
                System.out.println("  " + cardDescriptionForLog(c));
            }

            // Step 4: Check for declaration using strategy
            String declaration = checkComputerDeclaration(hand, nextPlayer);
            if (declaration != null) {
                setStatusText("Player " + nextPlayer + " is declaring " + declaration + "...");
                System.out.println("P0 DECLARING " + declaration + "!");
                addCardPlayedToLog(nextPlayer, selected, drawnCard, declaration);
            } else {
                System.out.println("P0 NOT declaring - continuing game");
                addCardPlayedToLog(nextPlayer, selected, drawnCard, null);
            }

            System.out.println("=== P0 COMPUTER TURN END ===\n");
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

    /**
     * Checks if current player has made a valid declaration
     * @return true if valid declaration ends the round
     */
    private boolean checkForDeclarations(int player) {
        // For Classic mode
        if (strategy instanceof ClassicRummyStrategy) {
            ClassicRummyStrategy classic = (ClassicRummyStrategy) strategy;
            if (classic.isRummyDeclared()) {
                System.out.println("RUMMY declared by P" + classic.getRummyDeclarer());
                return true;
            }
        }

        // For Gin mode
        if (strategy instanceof GinRummyStrategy) {
            GinRummyStrategy gin = (GinRummyStrategy) strategy;

            if (gin.isGinDeclared()) {
                System.out.println("GIN declared by P" + gin.getGinDeclarer());
                return true;
            }

            if (gin.isKnockDeclared()) {
                System.out.println("KNOCK declared by P" + gin.getKnocker());
                return true;
            }
        }

        return false;
    }

    private boolean playARound() {
        int nextPlayer = roundWinner;
        addRoundInfoToLog(currentRound);
        addPlayerCardsToLog();
        int i = 0;
        boolean isContinue = true;
        stockExhaustedThisRound = false;
        setupPlayerAutoMovements();

        while (isContinue) {
            addTurnInfoToLog(i);

            for (int j = 0; j < nbPlayers; j++) {
                Hand hand = hands[nextPlayer];

                // -------- Player turn (auto or human) --------
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
                                String declarationType = lastAction.name();

                                // Validate through strategy
                                boolean isValid = strategy.validateDeclaration(hands[nextPlayer], nextPlayer, declarationType);

                                if (isValid) {
                                    setStatus("Player " + nextPlayer + " is declaring " + declarationType + "...");
                                    isContinue = false;
                                    addCardPlayedToLog(nextPlayer, selected, card, declarationType);
                                    break;
                                } else {
                                    System.out.println("WARNING: Auto-script declared invalid " + declarationType);
                                    addCardPlayedToLog(nextPlayer, selected, card, null);
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
                } else {
                    processNonAutoPlaying(nextPlayer, hand);
                }

                // -------- Declarations handling --------
                // Check if any valid declaration was made
                boolean declarationMade = checkForDeclarations(nextPlayer);

                if (declarationMade) {
                    System.out.println("Valid declaration made - ending round");
                    isContinue = false;
                    break; // Exit the turn loop
                }

                // ----- Stockpile exhaustion check -----
                if (pack.isEmpty()) {
                    System.out.println("\n>>> STOCK EXHAUSTED <<<");
                    stockExhaustedThisRound = true;
                    setStatus("Stockpile is exhausted. Calculating players' scores now.");
                    isContinue = false;
                    break; // exit the inner for-loop immediately
                }

                // Advance to next player
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
        this.smartPlayer = new SmartComputerPlayer(deck);
        isAuto = Boolean.parseBoolean(properties.getProperty("isAuto"));
        thinkingTime = Integer.parseInt(properties.getProperty("thinkingTime", "200"));
        delayTime = Integer.parseInt(properties.getProperty("delayTime", "50"));

        // Initialize strategy based on mode from properties
        String mode = properties.getProperty("mode", "classic").toLowerCase();
        if (mode.equals("gin")) {
            this.strategy = new GinRummyStrategy(properties);
        } else {
            this.strategy = new ClassicRummyStrategy(properties);
        }

        // Get starting cards from strategy
        nbStartCards = strategy.getStartingCardCount();

        System.out.println("Initialized game with mode: " + strategy.getModeName());
    }

    private void calculateRoundScores() {
        System.out.println("\n========== CALCULATING ROUND SCORES ==========");
        System.out.println("Mode: " + strategy.getModeName());

        // Analyze all hands
        MeldDetector.MeldAnalysis[] analyses = new MeldDetector.MeldAnalysis[nbPlayers];
        for (int i = 0; i < nbPlayers; i++) {
            analyses[i] = MeldDetector.findBestMelds(hands[i]);
            System.out.println("P" + i + " melds=" + analyses[i].getMelds().size()
                    + " deadwood=" + analyses[i].getDeadwoodValue());
        }

        // Delegate scoring to strategy
        roundWinner = strategy.calculateRoundScores(hands, analyses, scores);

        // Update UI status based on strategy's decision
        setStatus("Round ended. P" + roundWinner + " wins!");

        System.out.println("New scores: P0=" + scores[0] + " P1=" + scores[1]);
        System.out.println("Next round starts with P" + roundWinner);
    }
}