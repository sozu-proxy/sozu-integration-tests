import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpResponse;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;
import org.testcontainers.utility.MountableFile;
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
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;
import static org.junit.Assert.*;
import static org.toilelibre.libe.curl.Curl.curl;


public class SozuContainerTest {

    private final static Logger log = Logger.getLogger(SozuContainerTest.class.getName());

    @ClassRule
    public static NodeBackendContainer nodeBackend = new NodeBackendContainer("172.18.0.5", Paths.get("node-backends/app-simple.js"), 8004);

    @ClassRule
    public static NodeBackendContainer nodeBackendHttp100 = new NodeBackendContainer("172.18.0.6", Paths.get("node-backends/app-http-continue.js"), 8005);

    @ClassRule
    public static NodeBackendContainer nodeWebsocket = new NodeBackendContainer("172.18.0.7", Paths.get("node-backends/server-websocket.js"), 8006);

    @Rule
    public SozuContainer sozuContainer = SozuContainer.newSozuContainer("2002:ac14::ff");

    private ToStringConsumer toStringSozuConsumer;

    @Rule
    // Use the test watcher to print the logs of sozu if a test failed
    public TestWatcher watcher = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            String sozuLogs = toStringSozuConsumer.toUtf8String();
            System.out.println(sozuLogs);
        }
    };

    public SozuContainerTest() throws URISyntaxException {}

    @Before
    public void beforeEach() {
        toStringSozuConsumer = new ToStringConsumer();
        sozuContainer.followOutput(toStringSozuConsumer, OutputFrame.OutputType.STDOUT);
    }

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

        nodeBackend1.stop();
        nodeBackend2.stop();
    }

    @Test
    public void testStickySessions () throws Exception {
        String appId = "stickysession";
        URL sozuUrl = sozuContainer.getBaseUrl("http", SozuContainer.DEFAULT_HTTP_PORT);


        // Set up an application with two backends, and sticky sessions with a specific sticky id (see config.toml)
        Backend backend1 = new Backend("rogue", "172.18.0.10", 8002);
        Backend backend2 = new Backend("war", "172.18.0.11", 8003);
        NodeBackendContainer nodeBackend1 = new NodeBackendContainer(backend1.getAddress(), Paths.get("node-backends/app-id.js"), backend1.getPort());
        NodeBackendContainer nodeBackend2 = new NodeBackendContainer(backend2.getAddress(), Paths.get("node-backends/app-id.js"), backend2.getPort());
        nodeBackend1.withEnv("ID", backend1.getId()).start();
        nodeBackend2.withEnv("ID", backend2.getId()).start();


        // We check that we got a sticky session cookie, and we store the id of the backend that answered
        String url = String.format("-H 'Connection: close' -H 'Host: %s.com' %s", appId, sozuUrl.toString());
        HttpResponse res = curl(url);
        String firstSozuBalanceId = res.getFirstHeader("Set-Cookie").getValue();
        String firstContent = IOUtils.toString(res.getEntity().getContent(), "UTF-8");


        // We check that we got a response from the same backend
        String urlWithCookie = String.format("-H 'Cookie: %s' -H 'Connection: close' -H 'Host: %s.com' %s", firstSozuBalanceId, appId, sozuUrl.toString());
        res = curl(urlWithCookie);

        String body = IOUtils.toString(res.getEntity().getContent(), "UTF-8");
        assertEquals(firstContent, body);


        // We remove all the backends and set up new ones
        sozuContainer.removeBackend(appId, backend1.getId(), backend1.getAddressWithPort());
        sozuContainer.removeBackend(appId, backend1.getId(), backend1.getAddressWithPort());
        Backend backend3 = new Backend("warlock", "172.18.0.12", 8004);
        NodeBackendContainer nodeBackend3 = new NodeBackendContainer(backend3.getAddress(), Paths.get("node-backends/app-id.js"), backend3.getPort());
        nodeBackend3.withEnv("ID", backend3.getId()).start();
        sozuContainer.addBackend(appId, backend3.getId(), backend3.getAddressWithPort());


        // We check that we got a response from a different backend, and a new session cookie
        res = curl(urlWithCookie);

        String bodyOfNewBackend = IOUtils.toString(res.getEntity().getContent(), "UTF-8");
        assertEquals(HTTP_OK, res.getStatusLine().getStatusCode());
        assertNotEquals(firstContent, bodyOfNewBackend);

        nodeBackend1.stop();
        nodeBackend2.stop();
        nodeBackend3.stop();
    }

    @Test
    public void testHttpsredirect() throws Exception {
        URL sozuUrl = sozuContainer.getBaseUrl("http", SozuContainer.DEFAULT_HTTP_PORT);
        int sozuHttpsPort = sozuContainer.getMappedPort(SozuContainer.DEFAULT_HTTPS_PORT);


        // Setup the backend with app-x-forwarded-proto.js as binary
        Backend backend = new Backend("paladin", "172.18.0.14", 8006);
        NodeBackendContainer nodeBackend = new NodeBackendContainer(backend.getAddress(), Paths.get("node-backends/app-x-forwarded-proto.js"), backend.getPort());
        nodeBackend.start();
        sozuContainer.addBackend("httpsredirect", backend.getId(), backend.getAddressWithPort());


        // Verify that the proxy answers with a 301 to the HTTPS version
        HttpResponse res = curl("-H 'Host: httpsredirect.com' " + sozuUrl.toString());
        assertEquals(HTTP_MOVED_PERM, res.getStatusLine().getStatusCode());

        String location = res.getFirstHeader("Location").getValue();
        assertEquals("https://httpsredirect.com/", location);


        // The client does a HTTPS request
        // FIXME We set in a magic string the ip gateway of the bridge network until #17 is fixed
        // TODO Maybe we should move the /certs folder in a better place
        Process p = Runtime.getRuntime().exec("curl -s --cacert ./src/test/resources/certs/CA.pem --resolve httpsredirect.com:" + sozuHttpsPort + ":172.18.0.1 https://httpsredirect.com:" + sozuHttpsPort);
        String stdout = IOUtils.toString(p.getInputStream(), "UTF-8");
        String stderr = IOUtils.toString(p.getErrorStream(), "UTF-8");


        // Verify that the server gets the correct protocol in the Forwarded-* headers
        if(!stdout.isEmpty()) {
            // The backend should return the x-forwarded-proto header content
            assertEquals("https", stdout);
        }
        else {
            log.log(Level.SEVERE, stderr);
            nodeBackend.stop();
            fail();
        }

        nodeBackend.stop();
    }

    @Test
    public void testConnectionWithIpv6() throws Exception {
        // Setup the backend with ipv6 address
        Backend backend = new Backend("paladin", "172.18.0.14", 8007);
        NodeBackendContainer nodeBackend = new NodeBackendContainer(backend.getAddress(), Paths.get("node-backends/app-simple.js"), backend.getPort());
        nodeBackend.setIpv6("2002:ac14::ff01");
        nodeBackend.start();

        //FIXME: find a java http client that can resolve a ipv6 address
        Process p = Runtime.getRuntime().exec("curl -s -g -6 --resolve ipv6.com:81:[2002:ac14::ff] http://ipv6.com:81");

        String stdout = IOUtils.toString(p.getInputStream(), "UTF-8");
        String stderr = IOUtils.toString(p.getErrorStream(), "UTF-8");

        // Verify that we get the backend response
        if(!stdout.isEmpty()) {
            assertEquals("Hello Node.js Server!", stdout);
        }
        else {
            log.log(Level.SEVERE, stderr);
            sozuContainer.stop();
            nodeBackend.stop();
            fail();
        }
    }

    @Test
    public void testchunkedResponse() throws Exception {
        String largeFilePath = "node-backends/lorem.txt";
        URL sozuUrl = sozuContainer.getBaseUrl("http", SozuContainer.DEFAULT_HTTP_PORT);

        Backend backend = new Backend("waagh", "172.18.0.13", 8005);
        NodeBackendContainer nodeBackend = new NodeBackendContainer(backend.getAddress(), Paths.get("node-backends/app-chunk-response.js"), backend.getPort());
        nodeBackend.withCopyFileToContainer(MountableFile.forClasspathResource(largeFilePath),"/");
        nodeBackend.withEnv("FILE", "lorem.txt");
        nodeBackend.start();


        // The server sends a file as chunked response
        HttpResponse res = curl("-H 'Host: chunkedresponse.com' " + sozuUrl.toString());


        // Verify if the client receives all the packets and check the file sha1sum
        assertEquals(HTTP_OK, res.getStatusLine().getStatusCode());
        InputStream inputStreamContent = res.getEntity().getContent();
        InputStream inputStreamFile = this.getClass().getClassLoader().getResourceAsStream(largeFilePath);

        String sha1Hex = DigestUtils.sha1Hex(inputStreamContent);
        String sha1HexExpected = DigestUtils.sha1Hex(inputStreamFile);
        assertEquals(sha1Hex, sha1HexExpected);

        nodeBackend.stop();
    }
}
