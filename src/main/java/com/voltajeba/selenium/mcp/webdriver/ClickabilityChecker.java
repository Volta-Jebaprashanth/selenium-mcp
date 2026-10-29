package com.voltajeba.selenium.mcp.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Map;

/**
 * Decides whether an element can actually be clicked: displayed, enabled, <em>and not covered by
 * another element</em> such as a loading overlay.
 * <p>
 * Selenium's own {@code elementToBeClickable} stops at displayed + enabled, which is why a click on an
 * element it calls clickable can still fail with {@code ElementClickInterceptedException}. The covered
 * check asks the page which element sits at the target's centre - the point WebDriver clicks - via
 * {@code elementFromPoint}, and reports what is on top when it isn't the target or inside it.
 * <p>
 * The check is a JavaScript function, {@link #STATE_FN}, shared with {@link PageSourceInspector} so
 * element listings report the same flags.
 */
public class ClickabilityChecker {

    /**
     * Defines {@code isInteractive(el)} and {@code interactState(el, scroll)}. The latter returns
     * {@code {visible, enabled, clickable[, coveredBy][, offscreen]}}. An element whose centre is outside
     * the viewport is scrolled to when {@code scroll} is true (as a click would); otherwise it is reported
     * {@code offscreen} with {@code clickable} left out, since what covers it can't be known unscrolled.
     */
    static final String STATE_FN = """
            var INTERACTIVE_TAGS = {A: 1, BUTTON: 1, INPUT: 1, SELECT: 1, TEXTAREA: 1, OPTION: 1, SUMMARY: 1};
            var INTERACTIVE_ROLES = {button: 1, link: 1, menuitem: 1, tab: 1, option: 1, checkbox: 1, radio: 1, 'switch': 1};

            function isInteractive(el) {
                if (INTERACTIVE_TAGS[el.tagName]) return el.tagName !== 'A' || el.hasAttribute('href') || el.hasAttribute('onclick');
                if (INTERACTIVE_ROLES[el.getAttribute('role')]) return true;
                return el.hasAttribute('onclick') || (el.hasAttribute('tabindex') && el.tabIndex >= 0);
            }

            function shortDescription(e) {
                var d = e.tagName.toLowerCase();
                if (e.id) d += '#' + e.id;
                var cls = typeof e.className === 'string' ? e.className.trim().split(/\\s+/).slice(0, 3) : [];
                if (cls.length && cls[0]) d += '.' + cls.join('.');
                return d;
            }

            function interactState(el, scroll) {
                var cs = window.getComputedStyle(el);
                var r = el.getBoundingClientRect();
                var visible = r.width > 0 && r.height > 0 && cs.visibility !== 'hidden'
                    && cs.visibility !== 'collapse' && parseFloat(cs.opacity) > 0;
                var enabled = !el.matches(':disabled') && el.getAttribute('aria-disabled') !== 'true'
                    && !el.closest('[inert]');
                var state = {visible: visible, enabled: enabled};
                if (!visible || !enabled) {
                    state.clickable = false;
                    return state;
                }
                var cx = r.left + r.width / 2, cy = r.top + r.height / 2;
                if (cx < 0 || cy < 0 || cx >= window.innerWidth || cy >= window.innerHeight) {
                    if (!scroll) {
                        state.offscreen = true;
                        return state;
                    }
                    el.scrollIntoView({block: 'center', inline: 'center'});
                    r = el.getBoundingClientRect();
                    cx = r.left + r.width / 2;
                    cy = r.top + r.height / 2;
                }
                var root = el.getRootNode();
                var hit = (root.elementFromPoint ? root : document).elementFromPoint(cx, cy);
                state.clickable = !hit || hit === el || el.contains(hit);
                if (!state.clickable) state.coveredBy = shortDescription(hit);
                return state;
            }
            """;

    private static final String CHECK_JS = STATE_FN + "return interactState(arguments[0], true);";

    /**
     * @param coveredBy a short description ({@code tag#id.class}) of the element on top, or {@code null}
     */
    public record Clickability(boolean visible, boolean enabled, boolean clickable, String coveredBy) {

        /**
         * @return {@code "true"}, or {@code "false"} with the reason, e.g. {@code "false: covered by div.loader"}
         */
        public String describe() {
            if (clickable) return "true";
            if (!visible) return "false: not displayed";
            if (!enabled) return "false: disabled";
            return "false: covered by " + coveredBy;
        }
    }

    private final WebDriver driver;

    public ClickabilityChecker(WebDriver driver) {
        this.driver = driver;
    }

    /**
     * Checks {@code element}, scrolling it into view first if its centre is outside the viewport.
     */
    public Clickability check(WebElement element) {
        Map<?, ?> state = (Map<?, ?>) ((JavascriptExecutor) driver).executeScript(CHECK_JS, element);
        return new Clickability(
                Boolean.TRUE.equals(state.get("visible")),
                Boolean.TRUE.equals(state.get("enabled")),
                Boolean.TRUE.equals(state.get("clickable")),
                (String) state.get("coveredBy"));
    }

    public Clickability check(By locator) {
        return check(driver.findElement(locator));
    }
}
