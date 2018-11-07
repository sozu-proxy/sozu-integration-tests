# Integration tests for Sozu

Experimental test suite for [Sozu](https://github.com/sozu-proxy/sozu)

# Requirements

## Packages

- Gradle 4.8 at least
- OpenJDK 1.8
- Docker 1.6.0 at least
- An environment with more than 2GB free disk space

NOTE: You don't have to install `gradle` to run the tests. This repository provide a gradle wrapper binary: `gradlew` that contain Gradle.

## Network (temporary until fixed)

You *must* create a local `bridge` network named "my-net":
`docker network create --driver=bridge --subnet=172.18.0.0/16 my-net`
Use this subnet in your Sozu `config.toml` for the backends address.

# Build

`gradle build` or `./gradlew build` with the gradle wrapper.

# Run the test suite

`gradle test` or `./gradlew test` (add `--info` or `--debug`) to run the tests suite.

This can take additional time to run the test suite the first time because of the download (and buid) of the docker images.
`testcontainers` doesn't check if the local docker image of your container as the same version as the one on docker hub.
So you have to `docker pull <your image>` if you work with docker image from docker HUB in your tests.


You can set the path to a local sozu Dockerfile with `SOZU_DOCKERFILE` env variable to avoid to download the sozu image from dockerhub.


NOTE: To avoid that you can build your container from a local `Dockerfile`:
This example use the `Classpath` to store the `Dockerfile`.

```java
public class MyContainer {

    public MyContainer() {
            super(
                new ImageFromDockerfile()
                    .withFileFromClasspath("Dockerfile", "path/to/Dockerfile")
            );
    }

}
```

# Add a new test `Class`

1. Create a new test file in `src/test/java`
2. Put all your config file in `src/test/resources` to retrieve them from the `Classpath`
3. Create a new test `Class`
4. Define the containers you want to run at the start of your test suite as fields of your `Class` and add `@Rule` on them
(that'll be use by `testcontainers`). You can still create them in the `@Before` or in your tests method.
5. `gradle test` or `./gradlew test` to run the test suite.

## Example

```java
public class MyContainer<SELF extends MyContainer<SELF>> extends GenericContainer<SELF> {


    public static final String IMAGE = "helloworld";
    public static final String DEFAULT_TAG = "latest";

    // Get image from dockerHUB
    public MyContainer() {
        super(IMAGE+ ":" + DEFAULT_TAG);
    }

    @Override
    protected void configure() {
        // use with* method to change the docker run command
    }

    // Build the URL to connect to your container with a mapped port
    public URL getBaseUrl(String scheme, int port) throws MalformedURLException {
        return new URL(scheme + "://" + getContainerIpAddress() + ":" + getMappedPort(port));
    }
}
```

# Add a new type of container

1. Create a new `Class` file in `src/main/java`
2. Extend them from GenericContainer: `<SELF extends NodeBackendContainer<SELF>> extends GenericContainer<SELF>`
3. (optional) To modify the docker image you have to do this in the constructor with the method `with*` e.g.: `withFileFromClasspath`
4. (optional) To modify the `docker run` command generate by testcontainers you have to do this in `@Override protected void configure()`

See [here](https://www.testcontainers.org/usage/options.html) for more information

NOTE: Use the `getMappedPort(<port>)` to get the host port mapped to the exposed port in the container.

## Example

```java
public class MyContainerTest {
    @Rule
    public MyContainer myContainer = new MyContainer();

    @Test
    public void testConnectionForMyContainer() throws Exception {
        URLConnection urlConnection = myContainer.getBaseUrl().openConnection();
        BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        String line = reader.readLine();
        assertEquals("A message", line);
    }
}

```

# Known issues

#### Can't get Docker image

Sometime `libcontainers` fail at download the docker images due to a network problem during the first run or when these are not already present in the host.
You can just replay the test suite to fix this.

```bash
  SozuContainerTest
    ✘ classMethod
      org.testcontainers.containers.ContainerFetchException: Can't get Docker image: org.testcontainers.images.builder.ImageFromDockerfile@1dfc4dc1

SozuContainerTest > classMethod FAILED
    org.testcontainers.containers.ContainerFetchException: Can't get Docker image: org.testcontainers.images.builder.ImageFromDockerfile@1dfc4dc1
        Caused by:
        com.github.dockerjava.api.exception.DockerClientException: Could not build image: The command '/bin/sh -c npm install' returned a non-zero code: 1
```