import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;
import strategy.EmptyWaitStrategy;
import utils.LoadBalancingPolicy;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class SozuContainer <SELF extends SozuContainer<SELF>> extends GenericContainer<SELF> {

    private final static Logger log = Logger.getLogger(SozuContainer.class.getName());

    public static final String IMAGE = "clevercloud/sozu";
    public static final String DEFAULT_TAG = "latest";

    public static final String MOUNT_POINT_CONFIG_FILE = "/etc/sozu/config.toml";

    public static final int DEFAULT_HTTPS_PORT = 443;
    public static final int DEFAULT_HTTP_PORT= 80;

    private Inet4Address ipv4;
    private Inet6Address ipv6;
    private String configFile;

    //Get image from docker HUB
    public SozuContainer(Inet4Address ipv4, Inet6Address ipv6, final String configFile) {
        super(IMAGE + ":" + DEFAULT_TAG);
        this.ipv4 = ipv4;
        this.ipv6 = ipv6;
        this.configFile = configFile;
    }

    //Build image from local Dockerfile in the Classpath
    public SozuContainer(final String pathToDockerFile, Inet4Address ipv4, Inet6Address ipv6, final String configFile) {
        super(new ImageFromDockerfile()
                .withFileFromClasspath("Dockerfile", pathToDockerFile));

        setWaitStrategy(new EmptyWaitStrategy());
        this.ipv4 = ipv4;
        this.ipv6 = ipv6;
        this.configFile = configFile;
    }

    @Override
    protected void configure() {
        withClasspathResourceMapping(this.configFile, MOUNT_POINT_CONFIG_FILE, BindMode.READ_ONLY);
        mapResourceParameterAsVolume("certs", "/"); //FIXME needed only for testHttpsredirect make this more configurable
        withNetworkMode("my-net");
        withCreateContainerCmdModifier(cmd ->
            cmd.withIpv4Address(this.ipv4.getHostAddress())
                //.withIpv6Address(this.ipv6.getHostAddress()) FIXME: got the error "user specified IP address is supported only when connecting to networks with user configured subnets" when IPv6 enabled
        );
    }

    @Override
    public Set<Integer> getLivenessCheckPortNumbers() {
        return Collections.singleton(getMappedPort(DEFAULT_HTTP_PORT));
    }

    static public SozuContainer newSozuContainer(Inet4Address ipv4, Inet6Address ipv6, String configFile) {
        String envPathSozuDockerfile = System.getenv("SOZU_DOCKERFILE");

        if (envPathSozuDockerfile == null || envPathSozuDockerfile.isEmpty()) {
            return new SozuContainer(ipv4, ipv6, configFile);
        }
        else {
            return new SozuContainer(envPathSozuDockerfile, ipv4, ipv6, configFile);
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
            add(MOUNT_POINT_CONFIG_FILE);
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
