package strategy;

import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;

import java.time.Duration;

public class EmptyWaitStrategy implements WaitStrategy {

    @Override
    public void waitUntilReady(WaitStrategyTarget waitStrategyTarget) {}

    @Override
    public WaitStrategy withStartupTimeout(Duration startupTimeout) {
        return null;
    }
}
