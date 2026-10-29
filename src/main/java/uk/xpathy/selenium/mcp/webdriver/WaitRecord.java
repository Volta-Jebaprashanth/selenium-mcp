package uk.xpathy.selenium.mcp.webdriver;

import java.time.Duration;

/**
 * What one action set off and what was waited for around it, as recorded by {@link AutoWaiter}.
 * <p>
 * The counts say what the action <em>triggered</em> - requests and animations that started after it -
 * however quickly they finished. That, not how long this session happened to wait, is what tells a
 * generated test it needs a wait at this step: a request that took 40ms against a local app can take
 * seconds in CI.
 *
 * @param coveredBy     what covered a click's target when the click was attempted, or {@code null}
 * @param coveredFor    how long the click then took to get through
 * @param requests      fetch/XHR requests started by the action ({@code -1} when not tracked)
 * @param animations    finite CSS/Web animations started by the action ({@code -1} when not tracked)
 * @param pending       requests still in flight when the network wait timed out
 * @param trackerLate   the action loaded a new page and the tracker could only be installed after it
 *                      loaded, so requests made during the load were not seen
 */
public record WaitRecord(int step, String action, String coveredBy, Duration coveredFor,
                         int requests, int animations,
                         Duration networkWaited, boolean networkTimedOut, int pending,
                         Duration animationWaited, boolean animationTimedOut,
                         boolean trackerLate) {

    public boolean tracked() {
        return requests >= 0;
    }

    public boolean needsClickableWait() {
        return coveredBy != null;
    }

    public boolean needsNetworkWait() {
        return requests > 0 || networkTimedOut;
    }

    public boolean needsAnimationWait() {
        return animations > 0 || animationTimedOut;
    }

    /**
     * @return whether the generated test needs a wait at this step
     */
    public boolean needsWait() {
        return needsClickableWait() || needsNetworkWait() || needsAnimationWait();
    }
}
