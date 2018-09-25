import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

public class SozuContainer <SELF extends SozuContainer<SELF>> extends GenericContainer<SELF> {

    private final static Logger LOGGER = Logger.getLogger(SozuContainer.class.getName());

    public static final String IMAGE = "clevercloud/sozu";
    public static final String DEFAULT_TAG = "latest";
    public static final int DEFAULT_HTTPS_PORT = 443;
    public static final int DEFAULT_HTTP_PORT= 80;

    public SozuContainer() {
        this(IMAGE + ":" + DEFAULT_TAG);
    }

    public SozuContainer(final String dockerImageName) {
        super(dockerImageName);
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
