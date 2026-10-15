package uk.xpathy.selenium.mcp.tools;

/**
 * Shared parameter descriptions for the optional {@code index}/{@code visibleOnly} arguments that
 * let element tools target the nth match of a locator instead of always the first.
 */
final class ElementMatch {

    static final String INDEX_DESC =
            "Zero-based index of the match to use when the locator matches several elements. Defaults to the first match.";
    static final String VISIBLE_ONLY_DESC =
            "Only consider elements that are currently displayed (applied before index). Defaults to false.";

    private ElementMatch() {
    }
}
