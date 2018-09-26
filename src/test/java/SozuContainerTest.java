import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.toilelibre.libe.curl.Curl.curl;


public class SozuContainerTest {

    private final static Logger LOGGER = Logger.getLogger(SozuContainerTest.class.getName());

    @Rule
    public NodeBackendContainer nodeBackend = new NodeBackendContainer("172.18.0.5");

    @Rule
    public SozuContainer sozuContainer = new SozuContainer();

    @Test
    public void testCorrectResponseFromSozu() throws Exception {
        URL sozuUrl = sozuContainer.getBaseUrl("http", SozuContainer.DEFAULT_HTTP_PORT);

        final HttpResponse curlResult = curl("-H 'Host: example.com' " + sozuUrl.toString());
        InputStream in = curlResult.getEntity().getContent();
        String body = IOUtils.toString(in, "UTF-8");

        assertEquals(HttpURLConnection.HTTP_OK, curlResult.getStatusLine().getStatusCode());
        assertEquals("Hello Node.js Server!", body);
    }
}