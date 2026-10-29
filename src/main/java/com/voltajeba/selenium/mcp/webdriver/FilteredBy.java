package com.voltajeba.selenium.mcp.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * A {@link By} that narrows another locator's matches to the visible ones and/or the one at a
 * zero-based index, so callers can say "the 3rd visible match" instead of crafting a more specific
 * selector. Because it is an ordinary {@link By}, every {@code By}-based method (clicks, waits,
 * screenshots, ...) accepts it unchanged. Built by {@link Locators#toBy(String, String, Integer, boolean)}.
 */
public class FilteredBy extends By {

    private final By base;
    private final Integer index;
    private final boolean visibleOnly;

    public FilteredBy(By base, Integer index, boolean visibleOnly) {
        if (index != null && index < 0) {
            throw new IllegalArgumentException("index must be 0 or greater, got " + index);
        }
        this.base = base;
        this.index = index;
        this.visibleOnly = visibleOnly;
    }

    @Override
    public List<WebElement> findElements(SearchContext context) {
        List<WebElement> matches = base.findElements(context);
        if (visibleOnly) {
            matches = matches.stream().filter(FilteredBy::isDisplayed).toList();
        }
        if (index == null) {
            return matches;
        }
        return index < matches.size() ? List.of(matches.get(index)) : List.of();
    }

    private static boolean isDisplayed(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (StaleElementReferenceException e) {
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(base.toString());
        if (visibleOnly) {
            sb.append(" [visible]");
        }
        if (index != null) {
            sb.append(" [index ").append(index).append(']');
        }
        return sb.toString();
    }
}
