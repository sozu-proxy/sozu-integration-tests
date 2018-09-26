import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.net.MalformedURLException;
import java.net.URL;

public class NodeBackendContainer <SELF extends NodeBackendContainer<SELF>> extends GenericContainer<SELF> {
    public static final Integer DEFAULT_PORT = 8080;

    public NodeBackendContainer() {
        super(
            new ImageFromDockerfile()
                .withFileFromClasspath("Dockerfile", "node-backend/Dockerfile")
                .withFileFromClasspath("app.js","node-backend/app.js")
        );
        setWaitStrategy(Wait.forHttp("/").forStatusCode(200));
    }

    @Override
    protected void configure() {
        addExposedPort(DEFAULT_PORT);
    }

    public URL getBaseUrl() throws MalformedURLException {
        return new URL("http://" + getContainerIpAddress() + ":" + getMappedPort(DEFAULT_PORT));
    }
}
