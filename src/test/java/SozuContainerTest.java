import org.apache.http.HttpResponse;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.toilelibre.libe.curl.Curl.curl;


public class SozuContainerTest {

    private final static Logger log = Logger.getLogger(SozuContainerTest.class.getName());

    @Rule
    public NodeBackendContainer nodeBackend = new NodeBackendContainer("172.18.0.5", Paths.get("node-backends/app-simple.js"), 8004);

    @Rule
    public NodeBackendContainer nodeBackendHttp100 = new NodeBackendContainer("172.18.0.6", Paths.get("node-backends/app-http-continue.js"), 8005);

    @Rule
    public NodeBackendContainer nodeWebsocket = new NodeBackendContainer("172.18.0.7", Paths.get("node-backends/server-websocket.js"), 8006);

    @Rule
    public SozuContainer sozuContainer = SozuContainer.newSozuContainer();

    public SozuContainerTest() throws URISyntaxException {}

    @Test
    public void testCorrectResponseFromSozu() throws Exception {
        URL sozuUrl = sozuContainer.getBaseUrl("http", SozuContainer.DEFAULT_HTTP_PORT);

        final HttpResponse curlResult = curl("-H 'Host: example.com' " + sozuUrl.toString());
        InputStream in = curlResult.getEntity().getContent();
        String body = IOUtils.toString(in, "UTF-8");

        assertEquals(HttpURLConnection.HTTP_OK, curlResult.getStatusLine().getStatusCode());
        assertEquals("Hello Node.js Server!", body);
    }

    @Test
    public void shouldNotPanicWhenHostIsEmpty() throws Exception {
        URL sozuUrl = sozuContainer.getBaseUrl("http", SozuContainer.DEFAULT_HTTP_PORT);

        final HttpResponse curlResult = curl("-H 'Host: ' " + sozuUrl.toString());

        assertEquals(HttpURLConnection.HTTP_NOT_FOUND, curlResult.getStatusLine().getStatusCode());
    }

    @Test
    public void shouldGet100ContinueStatusCode() throws Exception {
        URL sozuUrl = sozuContainer.getBaseUrl("http", 4000);

        final HttpResponse curlResult = curl("-H 'Host: continue.com' "
            + "-H 'Expect: 100-continue' "
            + "-H 'Content-Type: application/text' "
            + "-d 'yolo' "
            + sozuUrl.toString());

        //TODO: assert equal HTTP 100 (that need to use another HTTP client)
        assertEquals(200, curlResult.getStatusLine().getStatusCode());
    }

    @Test
    public void shouldWorkWithWebsocket() throws Exception {
        URI sozuUri = sozuContainer.getBaseUri("ws", 4001);

        CompletableFuture future = new CompletableFuture();
        final String messageEcho = "echo";

        WebSocketClient mWebSocketClient = new WebSocketClient(sozuUri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                this.send(messageEcho );
            }

            @Override
            public void onMessage(String msg) {
                future.complete(msg);
            }

            @Override
            public void onClose(int i, String s, boolean b) {
            }

            @Override
            public void onError(Exception e) {
            }
        };

        mWebSocketClient.connect();

        try {
            String res = (String) future.get(2, TimeUnit.SECONDS);
            assertEquals(messageEcho, res);
        } catch(TimeoutException e) {
            fail("We never receive the backend response");
        }
    }

    @Test
    public void testHttpCircuitBreaker() throws Exception {
        final HttpResponse res;

        ToStringConsumer toStringConsumer = new ToStringConsumer();
        sozuContainer.followOutput(toStringConsumer, OutputFrame.OutputType.STDOUT);

        URL sozuUrl = sozuContainer.getBaseUrl("http", SozuContainer.DEFAULT_HTTP_PORT);

        // We send multiple requests because of the CONN_RETRIES of sozu is define at 3
        // https://github.com/sozu-proxy/sozu/blob/218360d6b518747b70ea12dedb28b83456b54d26/lib/src/network/server.rs#L46
        curl("-H 'Host: circuit.com' " + sozuUrl.toString());
        res = curl("-H 'Host: circuit.com' " + sozuUrl.toString());

        assertEquals(HttpURLConnection.HTTP_UNAVAILABLE, res .getStatusLine().getStatusCode());

        String sozuLogs = toStringConsumer.toUtf8String();
        assertTrue(sozuLogs.contains("max connection attempt reached"));
    }

    @Test
    public void testRetryPolicy() throws Exception {
        URL sozuUrl = sozuContainer.getBaseUrl("http", SozuContainer.DEFAULT_HTTP_PORT);

        HttpResponse res = curl("-H 'Connection: close' -H 'Host: retry.com' " + sozuUrl.toString());

        assertEquals(HTTP_OK, res.getStatusLine().getStatusCode());
    }

    @Test
    public void testPathbegin() throws Exception {
        URL sozuUrl = sozuContainer.getBaseUrl("http", SozuContainer.DEFAULT_HTTP_PORT);

        // see config.toml
        String hostname = "pathbegin.com";
        String pathPrefix = "/api";
        String url = String.format("-H 'Host: %s' %s", hostname, sozuUrl.toString());

        HttpResponse res = curl(url);
        HttpResponse resWithPathBegin = curl(url + pathPrefix);

        assertEquals(HTTP_UNAVAILABLE, res.getStatusLine().getStatusCode());
        assertEquals(HTTP_OK, resWithPathBegin.getStatusLine().getStatusCode());
    }
}