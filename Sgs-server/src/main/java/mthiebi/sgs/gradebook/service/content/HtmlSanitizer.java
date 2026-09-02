package mthiebi.sgs.gradebook.service.content;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;

/**
 * Strips what a rich-text editor produces down to a fixed allowlist.
 * <p>
 * The brief asks for "standard text formatting", which means a WYSIWYG editor,
 * which means the console posts HTML. Storing that and later rendering it to a
 * parent is a **stored XSS hole**: the author is trusted, but the field is not -
 * anyone who can set homework could otherwise run script in every parent's
 * browser, and a compromised staff account would reach the whole school.
 * <p>
 * Applied on write, so nothing dangerous is ever stored. Sanitising only on read
 * would leave the original payload sitting in the database for whoever renders
 * it somewhere else later - an export, a mail template, a report - none of which
 * would know to be careful.
 * <p>
 * An allowlist, never a blocklist. There is no finite list of dangerous markup.
 */
@Component
public class HtmlSanitizer {

    /**
     * Exactly what the editor's toolbar can produce, and nothing else.
     * <p>
     * No style attribute: it carries url() and expression() and is the usual way
     * back in. No img: phase 8 has no image storage, and an img src is a request
     * to an arbitrary host from the reader's browser. No class or id either -
     * they would let content reach into the page's own stylesheet.
     */
    private static final PolicyFactory POLICY = new HtmlPolicyBuilder()
            .allowElements("p", "br", "strong", "b", "em", "i", "u", "s",
                    "ol", "ul", "li", "h3", "h4", "blockquote", "span")
            .allowElements("a")
            .allowAttributes("href").onElements("a")
            // http and https only. A javascript: href is a script the reader's
            // browser runs on click. Note allowStandardUrlProtocols() is
            // deliberately not called: it would add mailto back, which is how
            // an allowlist quietly stops being one.
            .allowUrlProtocols("http", "https")
            // Anything the school links to is external, and a link that can
            // script the opener via window.opener is a real hole.
            .requireRelNofollowOnLinks()
            .toFactory();

    /**
     * @return the cleaned HTML, or null when the input was null. Empty input
     * stays empty rather than becoming null: a caller clearing a body
     * and a caller not touching it are different intentions.
     */
    public String clean(String html) {
        if (html == null) {
            return null;
        }
        return POLICY.sanitize(html);
    }

    /**
     * A link the console offered, checked before it is stored.
     * <p>
     * Same reasoning as the href allowlist above, applied to the separate link
     * list - which does not go through the HTML policy at all, and would
     * otherwise be the unguarded way in.
     *
     * @return the trimmed url, or null when it is not one we will store
     */
    public String cleanUrl(String url) {
        if (url == null) {
            return null;
        }
        String trimmed = url.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String lower = trimmed.toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return null;
        }
        return trimmed;
    }
}
