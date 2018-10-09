import org.apache.http.HttpResponse;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;
import utils.Backend;

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

        res = curl("-H 'Host: circuit.com' " + sozuUrl.toString());

        assertEquals(HttpURLConnection.HTTP_UNAVAILABLE, res.getStatusLine().getStatusCode());

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

    @Test
    public void testPathbeginWithKeepAlive() throws Exception {
        URL sozuUrl = sozuContainer.getBaseUrl("http", SozuContainer.DEFAULT_HTTP_PORT);

        // see config.toml
        String hostname = "pathbegin.com";
        String pathPrefix = "/api";
        String url = String.format("-H 'Connection: keep-alive' -H 'Host: %s' %s", hostname, sozuUrl.toString());

        HttpResponse res = curl(url);
        HttpResponse resWithPathBegin = curl(url + pathPrefix);

        assertEquals(HTTP_UNAVAILABLE, res.getStatusLine().getStatusCode());
        assertEquals(HTTP_OK, resWithPathBegin.getStatusLine().getStatusCode());
    }

    @Test
    public void testRemoveBackendBetweenRequests() throws Exception {
        HttpResponse res;
        String body;
        String appId = "removebackendbetweenrequests";
        URL sozuUrl = sozuContainer.getBaseUrl("http", SozuContainer.DEFAULT_HTTP_PORT);


        Backend backend1 = new Backend("rogue", "172.18.0.8", 8000);
        Backend backend2 = new Backend("war", "172.18.0.9", 8001);
        NodeBackendContainer nodeBackend1 = new NodeBackendContainer(backend1.getAddress(), Paths.get("node-backends/app-id.js"), backend1.getPort());
        NodeBackendContainer nodeBackend2 = new NodeBackendContainer(backend2.getAddress(), Paths.get("node-backends/app-id.js"), backend2.getPort());
        nodeBackend1.withEnv("ID", backend1.getId()).start();
        nodeBackend2.withEnv("ID", backend2.getId()).start();

        String url = String.format("-H 'Connection: keep-alive' -H 'Host: %s.com' %s", appId, sozuUrl.toString());


        // Set up an application with one backend
        // Check that we receive a response, and that it comes from the first backend
        sozuContainer.addBackend(appId, backend1.getId(), backend1.getAddressWithPort());
        res = curl(url);
        assertEquals(HTTP_OK, res.getStatusLine().getStatusCode());
        body = IOUtils.toString(res.getEntity().getContent(), "UTF-8");
        assertEquals(backend1.getId(), body);


        // Change the application's configuration in sozu to remove the first backend and replace it with the second one
        sozuContainer.addBackend(appId, backend2.getId(), backend2.getAddressWithPort());
        sozuContainer.removeBackend(appId, backend1.getId(), backend1.getAddressWithPort());
        res = curl(url);
        // Check that we receive a response, and that it comes from the second backend
        assertEquals(HTTP_OK, res.getStatusLine().getStatusCode());
        body = IOUtils.toString(res.getEntity().getContent(), "UTF-8");
        assertEquals(backend2.getId(), body);
    }
}