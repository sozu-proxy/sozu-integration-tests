package strategy;

import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.logging.Logger;

public class HttpWaitStrategy implements WaitStrategy {
    URL baseUrl;

    private final static Logger log = Logger.getLogger(HttpWaitStrategy.class.getName());

    public HttpWaitStrategy(URL baseUrl) {
        this.baseUrl = baseUrl;
    }


    @Override
    public void waitUntilReady(WaitStrategyTarget waitStrategyTarget) {
        try {
            log.info("Trying to connect to " + this.baseUrl);
            HttpURLConnection connection = (HttpURLConnection) this.baseUrl.openConnection();
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
}
