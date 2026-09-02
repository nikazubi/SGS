package mthiebi.sgs.gradebook.content;

import mthiebi.sgs.gradebook.service.content.HtmlSanitizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What survives a rich-text field, and what must not.
 * <p>
 * The author is trusted; the field is not. Without this, anyone who can set
 * homework could run script in every parent's browser, and a compromised staff
 * account would reach the whole school.
 */
class HtmlSanitizerTest {

    private final HtmlSanitizer sanitizer = new HtmlSanitizer();

    // ---- what the toolbar produces has to survive --------------------------

    @Test
    @DisplayName("the editor's own formatting survives")
    void formattingSurvives() {
        String html = "<p>read <strong>chapter 4</strong> and <em>note</em> the "
                + "<u>dates</u></p><ul><li>one</li><li>two</li></ul>";
        assertEquals(html, sanitizer.clean(html));
    }

    @Test
    @DisplayName("Georgian passes through unharmed")
    void georgianSurvives() {
        // The whole system is Georgian; a sanitiser that mangled it would be
        // useless. Worth an explicit test after the varchar/nvarchar episode.
        String html = "<p>წაიკითხეთ <strong>მეოთხე თავი</strong></p>";
        assertEquals(html, sanitizer.clean(html));
    }

    @Test
    @DisplayName("an http link survives, and gets rel=nofollow")
    void linksSurvive() {
        String cleaned = sanitizer.clean("<a href=\"https://example.edu.ge/x\">materials</a>");
        assertTrue(cleaned.contains("https://example.edu.ge/x"), cleaned);
        // Anything the school links to is external, and a link that can script
        // the opener via window.opener is a real hole.
        assertTrue(cleaned.contains("nofollow"), cleaned);
    }

    // ---- what must not survive ---------------------------------------------

    @Test
    @DisplayName("a script tag does not survive")
    void scriptStripped() {
        String cleaned = sanitizer.clean("<p>hello</p><script>alert(1)</script>");
        assertFalse(cleaned.toLowerCase().contains("<script"), cleaned);
        assertFalse(cleaned.contains("alert(1)"), cleaned);
    }

    @Test
    @DisplayName("an event handler attribute does not survive")
    void eventHandlerStripped() {
        String cleaned = sanitizer.clean("<p onclick=\"steal()\">hello</p>");
        assertFalse(cleaned.toLowerCase().contains("onclick"), cleaned);
    }

    @Test
    @DisplayName("an img with onerror does not survive - element or handler")
    void imgOnerrorStripped() {
        // The classic payload. img is not on the allowlist at all in phase 8,
        // which has no image storage, so the whole element goes.
        String cleaned = sanitizer.clean("<img src=x onerror=alert(1)>");
        assertFalse(cleaned.toLowerCase().contains("onerror"), cleaned);
        assertFalse(cleaned.toLowerCase().contains("<img"), cleaned);
    }

    @Test
    @DisplayName("a javascript: href does not survive")
    void javascriptHrefStripped() {
        String cleaned = sanitizer.clean("<a href=\"javascript:alert(1)\">click</a>");
        assertFalse(cleaned.toLowerCase().contains("javascript:"), cleaned);
    }

    @Test
    @DisplayName("a style attribute does not survive")
    void styleStripped() {
        // style carries url() and is the usual way back in.
        String cleaned = sanitizer.clean("<p style=\"background:url(javascript:alert(1))\">x</p>");
        assertFalse(cleaned.toLowerCase().contains("style"), cleaned);
    }

    @Test
    @DisplayName("an iframe does not survive")
    void iframeStripped() {
        String cleaned = sanitizer.clean("<iframe src=\"https://evil.example\"></iframe>");
        assertFalse(cleaned.toLowerCase().contains("iframe"), cleaned);
    }

    // ---- the separate link list --------------------------------------------

    @Test
    @DisplayName("only http and https urls are stored")
    void urlProtocols() {
        assertEquals("https://example.edu.ge", sanitizer.cleanUrl("https://example.edu.ge"));
        assertEquals("http://example.edu.ge", sanitizer.cleanUrl(" http://example.edu.ge "));

        // The link list does not go through the HTML policy, so without its own
        // check it would be the unguarded way in.
        assertNull(sanitizer.cleanUrl("javascript:alert(1)"));
        assertNull(sanitizer.cleanUrl("JavaScript:alert(1)"));
        assertNull(sanitizer.cleanUrl("data:text/html;base64,PHNjcmlwdD4="));
        assertNull(sanitizer.cleanUrl("file:///etc/passwd"));
        assertNull(sanitizer.cleanUrl("   "));
        assertNull(sanitizer.cleanUrl(null));
    }

    @Test
    @DisplayName("null in, null out; empty stays empty")
    void nulls() {
        assertNull(sanitizer.clean(null));
        // Clearing a body and not touching it are different intentions, so an
        // empty string must not come back as null.
        assertEquals("", sanitizer.clean(""));
    }
}
