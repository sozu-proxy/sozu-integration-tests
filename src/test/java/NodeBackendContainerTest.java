import org.junit.Rule;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URLConnection;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NodeBackendContainerTest {

    private final static Logger LOGGER = Logger.getLogger(NodeBackendContainerTest.class.getName());

    @Rule
    public NodeBackendContainer nodeBackend = new NodeBackendContainer();

    @Test
    public void testCorrectResponseFromNodeBackend() throws Exception {
        LOGGER.info("Running node backend at " + nodeBackend.getBaseUrl());
        URLConnection urlConnection = nodeBackend.getBaseUrl().openConnection();
        BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        String line = reader.readLine();
        assertEquals("Hello Node.js Server!", line);
    }
}
