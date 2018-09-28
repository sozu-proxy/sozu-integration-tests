# Integration tests for Sozu

Experimental test suite for [Sozu](https://github.com/sozu-proxy/sozu)

# Requirements

- Gradle 4.8 at least
- OpenJDK 1.8
- Docker 1.6.0 at least
- An environment with more than 2GB free disk space

NOTE: You don't have to install `gradle` to run the tests. This repository provide a gradle wrapper binary: `gradlew` that contain Gradle.

# Build

`gradle build` or `./gradlew build` with the gradle wrapper.

# Run the test suite

`gradle test` or `./gradlew test` (add `--info` or `--debug`) to run the tests suite.

This can take additional time to run the test suite the first time because of the download (and buid) of the docker images.
`testcontainers` doesn't check if the local docker image of your container as the same version as the one on docker hub.
So you have to `docker pull <your image>` if you work with docker image from docker HUB in your tests.

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

# Add a new type of container

1. Create a new `Class` file in `src/main/java`
2. Extend them from GenericContainer: `<SELF extends NodeBackendContainer<SELF>> extends GenericContainer<SELF>`
3. (optional) To modify the docker image you have to do this in the constructor with the method `with*` e.g.: `withFileFromClasspath`
4. (optional) To modify the `docker run` command generate by testcontainers you have to do this in `@Override protected void configure()`

See [here](https://www.testcontainers.org/usage/options.html) for more information

NOTE: Use the `getMappedPort(<port>)` to get the host port mapped to the exposed port in the container.