import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;
import strategy.EmptyWaitStrategy;
import utils.LoadBalancingPolicy;

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

    private String ipv6;

    //Get image from docker HUB
    public SozuContainer(String ipv6) {
        super(IMAGE + ":" + DEFAULT_TAG);
        this.ipv6 = ipv6;
    }

    //Build image from local Dockerfile in the Classpath
    public SozuContainer(final String pathToDockerFile, final String ipv6) {
        super(new ImageFromDockerfile()
                .withFileFromClasspath("Dockerfile", pathToDockerFile));

        setWaitStrategy(new EmptyWaitStrategy());
        this.ipv6 = ipv6;
    }

    @Override
    protected void configure() {
        mapResourceParameterAsVolume("sozu", "/etc");
        mapResourceParameterAsVolume("certs", "/"); //FIXME needed only for testHttpsredirect make this more configurable
        withNetworkMode("my-net");
        withCreateContainerCmdModifier(cmd -> cmd.withIpv6Address(this.ipv6));
        addExposedPorts(DEFAULT_HTTP_PORT, DEFAULT_HTTPS_PORT, 4000, 4001, 81);
    }

    @Override
    public Set<Integer> getLivenessCheckPortNumbers() {
        return Collections.singleton(getMappedPort(DEFAULT_HTTP_PORT));
    }

    static public SozuContainer newSozuContainer(String ipv6) {
        String envPathSozuDockerfile = System.getenv("SOZU_DOCKERFILE");

        if (envPathSozuDockerfile == null || envPathSozuDockerfile.isEmpty()) {
            return new SozuContainer(ipv6);
        }
        else {
            return new SozuContainer(envPathSozuDockerfile, ipv6);
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

    public String removeBackend(String appId, String backendId, String backendAddress) {
        ArrayList args = new ArrayList() {{
            add("remove");
            add("-i");
            add(appId);
            add("--backend-id");
            add(backendId);
            add("-a");
            add(backendAddress);
        }};

        return execSozuctlCommand("backend", args);
    }

    public String addBackend(String appId, String backendId, String backendAddress) {
        ArrayList args = new ArrayList() {{
            add("add");
            add("-i");
            add(appId);
            add("--backend-id");
            add(backendId);
            add("-a");
            add(backendAddress);
        }};

        return execSozuctlCommand("backend", args);
    }

    public String addApplication(String appId, LoadBalancingPolicy lb) {
        ArrayList args = new ArrayList() {{
            add("add");
            add("-i");
            add(appId);
            add("--load-balancing-policy");
            add(lb.toString());
        }};

        return execSozuctlCommand("application", args);
    }

    public String removeApplication(String appId) {
        ArrayList args = new ArrayList() {{
            add("remove");
            add("--id");
            add(appId);
        }};

        return execSozuctlCommand("application", args);
    }
}
