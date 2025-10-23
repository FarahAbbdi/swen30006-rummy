package rummy;

import java.util.Properties;

/**
 * Singleton Factory for creating game mode strategies.
 * Centralizes strategy creation logic based on configuration.
 *
 * Design Pattern: Singleton Factory
 * - Ensures single factory instance
 * - Creates appropriate strategy based on mode string
 */
public class GameModeStrategyFactory {

    // Singleton instance
    private static GameModeStrategyFactory instance;

    /**
     * Private constructor to prevent instantiation
     */
    private GameModeStrategyFactory() {
        // Private to enforce singleton
    }

    /**
     * Gets the singleton instance of the factory
     * @return the factory instance
     */
    public static GameModeStrategyFactory getInstance() {
        if (instance == null) {
            instance = new GameModeStrategyFactory();
        }
        return instance;
    }

    /**
     * Creates the appropriate game mode strategy based on configuration
     *
     * @param mode The game mode ("classic", "gin", etc.)
     * @param properties The game properties configuration
     * @return The appropriate strategy implementation
     * @throws IllegalArgumentException if mode is not recognized
     */
    public GameModeStrategy createStrategy(String mode, Properties properties) {
        if (mode == null || mode.trim().isEmpty()) {
            throw new IllegalArgumentException("Game mode cannot be null or empty");
        }

        String normalizedMode = mode.toLowerCase().trim();

        switch (normalizedMode) {
            case "classic":
                return new ClassicRummyStrategy(properties);

            case "gin":
                return new GinRummyStrategy(properties);

            default:
                throw new IllegalArgumentException(
                        "Unknown game mode: '" + mode + "'. Supported modes: classic, gin"
                );
        }
    }

    /**
     * Creates strategy with default mode (classic)
     *
     * @param properties The game properties configuration
     * @return Classic Rummy strategy
     */
    public GameModeStrategy createDefaultStrategy(Properties properties) {
        return createStrategy("classic", properties);
    }
}