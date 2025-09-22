import org.junit.Test;
import rummy.PropertiesLoader;
import rummy.Rummy;

import java.util.Properties;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class TestGame {

    private String runningGame(String propertiesFile) {
        final Properties properties = PropertiesLoader.loadPropertiesFile(propertiesFile);
        String logResult = new Rummy(properties).runApp();
        return logResult;
    }

    @Test(timeout = 90000)
    public void testClassicRummy() {
        String testProperties = "properties/test1.properties";
        String logResult = runningGame(testProperties);
        assertTrue("Round 0 ends with P1 wins with a total of 103 points", logResult.contains("Round0 End:P0-0,P1-103"));
        assertTrue("Game Ends with P1 win", logResult.contains("Game End:P1"));
    }

    @Test(timeout = 90000)
    public void testGinKnock() {
        String testProperties = "properties/test2.properties";
        String logResult = runningGame(testProperties);
        assertTrue("Round 0 ends with P1 wins with a total of 33 points", logResult.contains("Round0 End:P0-0,P1-33"));
        assertTrue("Round 1 ends with P1 wins with a total of 117 points", logResult.contains("Round1 End:P0-0,P1-117"));
        assertTrue("Game Ends with P1 win", logResult.contains("Game End:P1"));
    }

    @Test(timeout = 90000)
    public void testMeldFormAndRummy() {
        String testProperties = "properties/test3.properties";
        String logResult = runningGame(testProperties);
        assertTrue("Round 0 ends with P0 wins with a total of 102 points", logResult.contains("Round0 End:P0-102,P1-0"));
        assertTrue("Game Ends with P0 win", logResult.contains("Game End:P0"));
    }

    @Test(timeout = 90000)
    public void testSmartComputers() {
        String testProperties = "properties/test4.properties";
        String logResult = runningGame(testProperties);
        assertTrue("Turn 0: P0 needs to keep 5S and discard 9D", logResult.contains("Turn0:P1-8H-5S,P0-5S-9D"));
        assertTrue("Turn 1: P0 needs to keep 6S and discard 13S", logResult.contains("Turn1:P1-9D-6S,P0-6S-13S"));
        assertTrue("Turn 2: P0 needs to keep 7D and discard 2S", logResult.contains("Turn2:P1-13S-7D,P0-7D-2S"));
        assertTrue("Round 0 ends with P0 wins with a total of 100 points", logResult.contains("Round0 End:P0-100,P1-0"));
        assertTrue("Game Ends with P0 win", logResult.contains("Game End:P0"));
    }

    @Test(timeout = 90000)
    public void testSmartComputersForGin() {
        String testProperties = "properties/test5.properties";
        String logResult = runningGame(testProperties);
        assertTrue("Turn 0: P0 needs to call Knock", logResult.contains("Turn0:P1-13S-12S,P0-12S-2H-KNOCK"));
        assertTrue("Round 0 ends with P0 wins with a total of 66 points", logResult.contains("Round0 End:P0-66,P1-0"));
        assertTrue("Round 1 ends with P0 wins with a total of 150 points", logResult.contains("Round1 End:P0-150,P1-0"));
        assertTrue("Game Ends with P0 win", logResult.contains("Game End:P0"));
    }
}
