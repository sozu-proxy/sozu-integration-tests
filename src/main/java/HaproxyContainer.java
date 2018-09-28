import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;
import strategy.HttpWaitStrategy;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

public class HaproxyContainer <SELF extends NodeBackendContainer<SELF>> extends GenericContainer<SELF> {

    private final static Logger log = Logger.getLogger(SozuContainer.class.getName());

    public static final String IMAGE = "haproxy";

    public static final int DEFAULT_HTTPS_PORT = 443;
    public static final int DEFAULT_HTTP_PORT= 80;

    public HaproxyContainer() {
        super(
            new ImageFromDockerfile()
                .withFileFromClasspath("Dockerfile", "haproxy/Dockerfile")
                .withFileFromClasspath("haproxy.cfg","haproxy/haproxy.cfg")
        );
    }

    @Override
    protected void configure() {
        //configure network
        withNetworkMode("my-net");
        addExposedPorts(DEFAULT_HTTP_PORT, DEFAULT_HTTPS_PORT);

        withCommand("haproxy -f /etc/haproxy/haproxy.cfg");
    }

    @Override
    public Set<Integer> getLivenessCheckPortNumbers() {
        return Collections.singleton(getMappedPort(DEFAULT_HTTP_PORT));
    }

    public URL getBaseUrl(String scheme, int port) throws MalformedURLException {
        return new URL(scheme + "://" + getContainerIpAddress() + ":" + getMappedPort(port));
    }

    private void mapResourceParameterAsVolume(String paramName, String pathNameInContainer) {
        final MountableFile mountableFile = MountableFile.forClasspathResource(paramName);
        withCopyFileToContainer(mountableFile, pathNameInContainer);
    }
}
