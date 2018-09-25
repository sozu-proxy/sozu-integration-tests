import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URLConnection;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class SozuContainerTest {

    private final static Logger LOGGER = Logger.getLogger(SozuContainerTest.class.getName());

    @Rule
    public SozuContainer sozuContainer = new SozuContainer();

    @Test
    public void testCorrectResponseFromSozu() throws Exception {
        LOGGER.info("Running sozu proxy at" + sozuContainer.getBaseUrl("http", SozuContainer.DEFAULT_HTTP_PORT));
    }
}