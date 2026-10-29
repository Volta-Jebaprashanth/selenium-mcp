package com.voltajeba.selenium.mcp.webdriver;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * A session's wait strategy, applied by {@link AutoWaiter}: an implicit wait before each element lookup
 * (Selenium's own {@code implicitlyWait}), and after each action an optional wait for the network to go
 * quiet for {@code networkIdle}, then for animations to stay still for {@code animationIdle}. A zero
 * idle time turns that wait off; {@link #NONE} is plain Selenium behaviour.
 *
 * @param ignoredUrls URL substrings whose requests don't count as network activity (analytics, long polling)
 */
public record AutoWaitSettings(Duration implicitWait, Duration networkIdle, Duration networkTimeout,
                               Duration animationIdle, Duration animationTimeout, List<String> ignoredUrls) {

    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    public static final AutoWaitSettings NONE = new AutoWaitSettings(
            Duration.ZERO, Duration.ZERO, DEFAULT_TIMEOUT, Duration.ZERO, DEFAULT_TIMEOUT, List.of());

    public AutoWaitSettings {
        Objects.requireNonNull(implicitWait, "implicitWait");
        Objects.requireNonNull(networkIdle, "networkIdle");
        Objects.requireNonNull(networkTimeout, "networkTimeout");
        Objects.requireNonNull(animationIdle, "animationIdle");
        Objects.requireNonNull(animationTimeout, "animationTimeout");
        requireNotNegative(implicitWait, "implicitWait");
        requireNotNegative(networkIdle, "networkIdle");
        requireNotNegative(networkTimeout, "networkTimeout");
        requireNotNegative(animationIdle, "animationIdle");
        requireNotNegative(animationTimeout, "animationTimeout");
        ignoredUrls = ignoredUrls == null
                ? List.of()
                : ignoredUrls.stream().filter(Objects::nonNull).map(String::strip).filter(s -> !s.isEmpty()).toList();
    }

    public boolean waitsForNetwork() {
        return !networkIdle.isZero();
    }

    public boolean waitsForAnimations() {
        return !animationIdle.isZero();
    }

    /**
     * @return whether anything happens after an action, and so whether the page tracker is needed
     */
    public boolean waitsAfterActions() {
        return waitsForNetwork() || waitsForAnimations();
    }

    public boolean isNone() {
        return implicitWait.isZero() && !waitsAfterActions();
    }

    private static void requireNotNegative(Duration value, String name) {
        if (value.isNegative()) {
            throw new IllegalArgumentException(name + " must be 0 or more, got " + value);
        }
    }
}
