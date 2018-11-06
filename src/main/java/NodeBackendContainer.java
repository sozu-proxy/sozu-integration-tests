import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

public class NodeBackendContainer <SELF extends NodeBackendContainer<SELF>> extends GenericContainer<SELF> {
    public static final Integer DEFAULT_PORT = 8080;
    private String ipv4;
    private String ipv6 = "";
    private int portHttp;

    public NodeBackendContainer(String ipv4, Path path, int portHttp) {
        super(
            new ImageFromDockerfile()
                    .withFileFromClasspath("app.js", path.toString())
                    .withFileFromClasspath("package.json", "node-backends/package.json")
                    .withDockerfileFromBuilder(builder ->
                        builder
                            .from("node:latest")
                            .copy("app.js", "app.js")
                            .copy("package.json", "package.json")
                            .expose(portHttp)
                            .run("npm install")
                            .cmd("node", "app.js")
                            .build()
                        )
        );

        setWaitStrategy(Wait.forListeningPort());

        this.ipv4 = ipv4;
        this.portHttp = portHttp;
    }

    @Override
    protected void configure() {
        addExposedPort(portHttp);
        withNetworkMode("my-net");
        withEnv("PORT", String.valueOf(portHttp));
        withCreateContainerCmdModifier(cmd -> cmd.withIpv4Address(this.ipv4));

        if (!this.ipv6.isEmpty())
            withCreateContainerCmdModifier(cmd -> cmd.withIpv6Address(this.ipv6));
    }

    public URL getBaseUrl() throws MalformedURLException {
        return new URL("http://" + getContainerIpAddress() + ":" + getMappedPort(portHttp));
    }

    public void setIpv6(String ipv6) {
        this.ipv6 = ipv6;
    }
}
