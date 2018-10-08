import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;
import strategy.EmptyWaitStrategy;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class SozuContainer <SELF extends SozuContainer<SELF>> extends GenericContainer<SELF> {

    private final static Logger log = Logger.getLogger(SozuContainer.class.getName());

    public static final String IMAGE = "clevercloud/sozu";
    public static final String DEFAULT_TAG = "latest";

    public static final int DEFAULT_HTTPS_PORT = 443;
    public static final int DEFAULT_HTTP_PORT= 80;

    //Get image from docker HUB
    public SozuContainer() {
        super(IMAGE + ":" + DEFAULT_TAG);
    }

    //Build image from local Dockerfile in the Classpath
    public SozuContainer(final String pathToDockerFile) {
        super(new ImageFromDockerfile()
                .withFileFromClasspath("Dockerfile", pathToDockerFile));

        setWaitStrategy(new EmptyWaitStrategy());
    }

    @Override
    protected void configure() {
        mapResourceParameterAsVolume("sozu", "/etc");
        withNetworkMode("my-net");
        addExposedPorts(DEFAULT_HTTP_PORT, DEFAULT_HTTPS_PORT, 4000, 4001);
    }

    @Override
    public Set<Integer> getLivenessCheckPortNumbers() {
        return Collections.singleton(getMappedPort(DEFAULT_HTTP_PORT));
    }

    static public SozuContainer newSozuContainer() {
        String envPathSozuDockerfile = System.getenv("SOZU_DOCKERFILE");

        if (envPathSozuDockerfile == null || envPathSozuDockerfile.isEmpty()) {
            return new SozuContainer();
        }
        else {
            return new SozuContainer(envPathSozuDockerfile);
        }
    }

    private void mapResourceParameterAsVolume(String paramName, String pathNameInContainer) {
        final MountableFile mountableFile = MountableFile.forClasspathResource(paramName);
        withCopyFileToContainer(mountableFile, pathNameInContainer);
    }

    public URL getBaseUrl(String scheme, int port) throws MalformedURLException {
        return new URL(scheme + "://" + getContainerIpAddress() + ":" + getMappedPort(port));
    }

    public URI getBaseUri(String scheme, int port) throws URISyntaxException {
        return new URI(scheme + "://" + getContainerIpAddress() + ":" + getMappedPort(port));
    }

    public String execSozuctlCommand(String command, List<String> args) {
        ArrayList sozuctlCmd = new ArrayList() {{
            //TODO: change the WORKDIR in Dockerfile to avoid to use ../../
            add("../../sozuctl");
            add("-c");
            add("/etc/sozu/config.toml");
            add(command);
            addAll(args);
        }};

        String [] sozuctlCmdTmp = (String[]) sozuctlCmd.toArray(new String[sozuctlCmd.size()]); // nice Java

        try {
            ExecResult res = this.execInContainer(sozuctlCmdTmp);
            return res.getStdout();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return "";
    }
}
