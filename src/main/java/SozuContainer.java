import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;
import java.net.HttpURLConnection;


public class SozuContainer <SELF extends SozuContainer<SELF>> extends GenericContainer<SELF> {

    private final static Logger log = Logger.getLogger(SozuContainer.class.getName());

    public static final String IMAGE = "clevercloud/sozu";
    public static final String DEFAULT_TAG = "latest";
    public static final int DEFAULT_HTTPS_PORT = 443;
    public static final int DEFAULT_HTTP_PORT= 80;

    public SozuContainer() {
        this(IMAGE + ":" + DEFAULT_TAG);
    }

    public SozuContainer(final String dockerImageName) {
        super(dockerImageName);

        setWaitStrategy(new WaitStrategy() {
            @Override
            public void waitUntilReady(WaitStrategyTarget waitStrategyTarget) {
                try {
                    log.info("Trying to connect to " + getBaseUrl("http", 80));
                    HttpURLConnection connection = (HttpURLConnection) getBaseUrl("http", 80).openConnection();
                    connection.setRequestMethod("GET");
                    connection.connect();
                    log.info("Get response code " + connection.getResponseCode());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public WaitStrategy withStartupTimeout(Duration startupTimeout) {
                return null;
            }
        });
    }

    @Override
    protected void configure() {
        mapResourceParameterAsVolume("sozu", "/etc");
        addExposedPort(80);
        addExposedPort(443);
    }

    @Override
    protected Integer getLivenessCheckPort() {
        return getMappedPort(80);
    }

    private void mapResourceParameterAsVolume(String paramName, String pathNameInContainer) {
        final MountableFile mountableFile = MountableFile.forClasspathResource(paramName);
        withCopyFileToContainer(mountableFile, pathNameInContainer);
    }

    public URL getBaseUrl(String scheme, int port) throws MalformedURLException {
        return new URL(scheme + "://" + getContainerIpAddress() + ":" + getMappedPort(port));
    }
}
