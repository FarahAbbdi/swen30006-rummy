package rummy;

import java.util.Properties;
import java.util.Random;

class GameConfig {
    static final int seed = 30008;
    static final Random random = new Random(seed);

    final String version = "1.0";
    final int nbPlayers = 2;

    final boolean isAuto;
    final boolean isGinMode;
    final int knockThreshold;
    final int nbStartCards;
    final int thinkingTime;
    final int delayTime;

    final Properties properties;

    GameConfig(Properties props) {
        this.properties = props;

        isAuto = Boolean.parseBoolean(props.getProperty("isAuto"));
        thinkingTime = Integer.parseInt(props.getProperty("thinkingTime", "200"));
        delayTime    = Integer.parseInt(props.getProperty("delayTime", "50"));

        String mode = props.getProperty("mode", "classic").toLowerCase();
        isGinMode = mode.equals("gin");
        nbStartCards = isGinMode ? 10 : Integer.parseInt(props.getProperty("number_cards", "13"));
        knockThreshold = Integer.parseInt(props.getProperty("knock_threshold", "7"));
    }
}