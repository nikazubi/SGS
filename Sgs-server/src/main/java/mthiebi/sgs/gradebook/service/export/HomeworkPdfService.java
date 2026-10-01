package mthiebi.sgs.gradebook.service.export;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.gradebook.service.content.PostDraft;
import mthiebi.sgs.gradebook.service.content.PostView;
import mthiebi.sgs.gradebook.service.parent.ParentContentView;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A day of homework as a PDF, for parents who want it on paper or in a folder.
 * <p>
 * Server-side rather than in the console, for one reason above the others: the
 * text is Georgian. A PDF cannot fall back to a font the reader happens to
 * have - whatever glyphs are not embedded are not in the file at all - so the
 * document has to be built somewhere the font is guaranteed, and that is here.
 * The browser-side PDF libraries ship Latin-1 base fonts and would have
 * produced a page of blanks.
 */
@Service
public class HomeworkPdfService {

    private static final String FONT_REGULAR = "/pdf/NotoSansGeorgian-Regular.ttf";
    private static final String FONT_BOLD = "/pdf/NotoSansGeorgian-Bold.ttf";
    /**
     * The login page's logo, not the header bar's. The header one is white
     * artwork meant to sit on the blue bar, and on a white page it is all but
     * invisible - which is exactly how the first render of this came out.
     */
    private static final String LOGO = "/pdf/logo.png";

    /** The header bar's blue, so the document is recognisably the school's. */
    private static final BaseColor BRAND = new BaseColor(0x01, 0x61, 0x9b);
    private static final BaseColor RULE = new BaseColor(0xcc, 0xdd, 0xe8);
    private static final BaseColor MUTED = new BaseColor(0x55, 0x60, 0x6a);

    /**
     * Loaded once. {@code createFont} parses and subsets the file, which is not
     * something to repeat per download, and iText's own cache is keyed on a
     * filename we are not using - the font arrives as bytes from the jar.
     */
    private volatile Fonts fonts;

    private static final class Fonts {
        private final Font h1;
        private final Font subject;
        private final Font itemTitle;
        private final Font body;
        private final Font bodyBold;
        private final Font bodyItalic;
        private final Font small;
        private final Font link;

        private Fonts(BaseFont regular, BaseFont bold) {
            this.h1 = new Font(bold, 17, Font.NORMAL, BRAND);
            this.subject = new Font(bold, 12.5f, Font.NORMAL, BRAND);
            this.itemTitle = new Font(bold, 11);
            this.body = new Font(regular, 10.5f);
            this.bodyBold = new Font(bold, 10.5f);
            this.bodyItalic = new Font(regular, 10.5f, Font.ITALIC);
            this.small = new Font(regular, 9, Font.NORMAL, MUTED);
            this.link = new Font(regular, 9.5f, Font.UNDERLINE, BRAND);
        }
    }

    /**
     * The renderer. Everything else here assembles one of these.
     */
    public byte[] render(HomeworkPdfDoc source) throws SGSException {

        if (source.isEmpty()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "დავალება ვერ მოიძებნა");
        }

        Fonts f = fonts();
        Document doc = new Document(PageSize.A4, 46, 46, 40, 44);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            header(doc, f, source.getDateLine(), source.getContextLine());

            for (HomeworkPdfDoc.Section section : source.getSections()) {
                if (section.getItems().isEmpty()) {
                    continue;
                }
                doc.add(subjectHeading(section.getSubjectName(), f));
                boolean first = true;
                for (HomeworkPdfDoc.Item item : section.getItems()) {
                    item(doc, f, item, first);
                    first = false;
                }
            }

            doc.close();
        } catch (DocumentException e) {
            throw new SGSException(SGSExceptionCode.INTERNAL_SEVER_ERROR,
                    "PDF-ის შექმნა ვერ მოხერხდა");
        }
        return out.toByteArray();
    }

    /**
     * The parent portal's day, as a document.
     *
     * @param day         the day as the portal already assembles it
     * @param studentLine "name — class", or null to leave the line out
     * @param onlyUuid    one assignment instead of the whole day, or null for all
     */
    public byte[] renderParentDay(ParentContentView.HomeworkDayDetail day,
                                  String studentLine,
                                  String onlyUuid) throws SGSException {

        HomeworkPdfDoc source = new HomeworkPdfDoc(formatDate(day.getDate()), studentLine);

        for (ParentContentView.HomeworkSubject subject : day.getSubjects()) {
            HomeworkPdfDoc.Section section =
                    new HomeworkPdfDoc.Section(subject.getSubjectName());
            for (ParentContentView.HomeworkItem item : subject.getItems()) {
                // Filtered here rather than in the query, so a single item keeps
                // the subject heading that gives it meaning.
                if (onlyUuid != null && !onlyUuid.isEmpty()
                        && !onlyUuid.equals(item.getUuid())) {
                    continue;
                }
                HomeworkPdfDoc.Item out = new HomeworkPdfDoc.Item(
                        item.getTitle(), item.getBodyHtml(), null);
                for (ParentContentView.Link link : item.getLinks()) {
                    out.getLinks().add(
                            new HomeworkPdfDoc.Link(link.getUrl(), link.getLabel()));
                }
                section.getItems().add(out);
            }
            if (!section.getItems().isEmpty()) {
                source.getSections().add(section);
            }
        }
        return render(source);
    }

    /**
     * The staff console's list, as a document.
     * <p>
     * Grouped by subject here rather than by the caller, because the list
     * arrives flat and the page it prints to is organised by subject either
     * way. Order is preserved, so the document follows the screen.
     *
     * @param posts       what the console is showing, in its own order
     * @param contextLine the class, and the filter it was taken from
     * @param dateLine    the range, already formatted
     */
    public byte[] renderStaffList(List<PostView> posts,
                                  String contextLine,
                                  String dateLine) throws SGSException {

        HomeworkPdfDoc source = new HomeworkPdfDoc(dateLine, contextLine);
        Map<String, HomeworkPdfDoc.Section> bySubject = new LinkedHashMap<>();

        for (PostView post : posts) {
            String subject = post.getSubjectName() == null ? "" : post.getSubjectName();
            HomeworkPdfDoc.Section section = bySubject.computeIfAbsent(subject, name -> {
                HomeworkPdfDoc.Section created = new HomeworkPdfDoc.Section(name);
                source.getSections().add(created);
                return created;
            });

            HomeworkPdfDoc.Item item = new HomeworkPdfDoc.Item(
                    post.getTitle(), post.getBodyHtml(), staffNote(post));
            for (PostDraft.LinkDraft link : post.getLinks()) {
                item.getLinks().add(
                        new HomeworkPdfDoc.Link(link.getUrl(), link.getLabel()));
            }
            section.getItems().add(item);
        }
        return render(source);
    }

    /**
     * The line under a staff assignment's title: its date, and whether parents
     * have it.
     * <p>
     * The state is spelled out rather than left to the reader. This list mixes
     * drafts with published work, and a printed page has no colour coding to
     * fall back on - an unsent draft that looks identical to a sent one is how
     * a teacher concludes they have already set work they have not.
     */
    private String staffNote(PostView post) {
        StringBuilder note = new StringBuilder();
        if (post.getEventDate() != null) {
            note.append(formatDate(post.getEventDate().toString()));
        }
        String state;
        if (!"PUBLISHED".equals(post.getStatus())) {
            state = "დრაფტი — გაგზავნილი არ არის";
        } else if (post.isHasUnpublishedChanges()) {
            state = "შეცვლილია გამოქვეყნების შემდეგ";
        } else {
            state = "გამოქვეყნებული";
        }
        if (note.length() > 0) {
            note.append("  ·  ");
        }
        return note.append(state).toString();
    }

    private void header(Document doc, Fonts f, String dateLine, String contextLine)
            throws DocumentException {

        PdfPTable bar = new PdfPTable(2);
        bar.setWidthPercentage(100);
        bar.setWidths(new float[]{62, 38});

        PdfPCell left = new PdfPCell();
        left.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
        left.setPaddingTop(2);
        left.addElement(new Paragraph("დავალება", f.h1));
        if (dateLine != null && !dateLine.isEmpty()) {
            Paragraph when = new Paragraph(dateLine, f.body);
            when.setSpacingBefore(2);
            left.addElement(when);
        }
        if (contextLine != null && !contextLine.isEmpty()) {
            left.addElement(new Paragraph(contextLine, f.small));
        }
        bar.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Image logo = logo();
        if (logo != null) {
            logo.scaleToFit(118, 44);
            logo.setAlignment(Element.ALIGN_RIGHT);
            right.addElement(logo);
        }
        bar.addCell(right);
        doc.add(bar);

        // A rule rather than a filled band: this gets printed, often on a home
        // printer, and a solid blue header is a lot of somebody's ink.
        PdfPTable rule = new PdfPTable(1);
        rule.setWidthPercentage(100);
        rule.setSpacingBefore(9);
        rule.setSpacingAfter(4);
        PdfPCell line = new PdfPCell(new Phrase(""));
        line.setFixedHeight(1.2f);
        line.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
        line.setBackgroundColor(RULE);
        rule.addCell(line);
        doc.add(rule);
    }

    private Paragraph subjectHeading(String name, Fonts f) {
        Paragraph p = new Paragraph(name == null ? "" : name, f.subject);
        p.setSpacingBefore(13);
        p.setSpacingAfter(3);
        return p;
    }

    /**
     * @param first the subject's first assignment, which sits under its heading
     *              and so needs less air above it than the ones that follow -
     *              without the gap two assignments read as one long one
     */
    private void item(Document doc, Fonts f, HomeworkPdfDoc.Item item,
                      boolean first) throws DocumentException {

        List<Paragraph> parts = new ArrayList<>();

        if (item.getTitle() != null && !item.getTitle().isEmpty()) {
            parts.add(new Paragraph(item.getTitle(), f.itemTitle));
        }
        if (item.getNote() != null && !item.getNote().isEmpty()) {
            Paragraph note = new Paragraph(item.getNote(), f.small);
            note.setSpacingAfter(1);
            parts.add(note);
        }
        parts.addAll(html(item.getBodyHtml(), f));

        for (HomeworkPdfDoc.Link link : item.getLinks()) {
            Paragraph p = new Paragraph();
            p.setIndentationLeft(10);
            p.setSpacingBefore(2);
            String label = link.getLabel() == null || link.getLabel().isEmpty()
                    ? link.getUrl() : link.getLabel();
            p.add(new Chunk(label + " ", f.body));
            // Printed out, a link nobody can click is only useful if the
            // address itself is on the page.
            p.add(new Chunk(link.getUrl(), f.link));
            parts.add(p);
        }

        if (!parts.isEmpty()) {
            parts.get(0).setSpacingBefore(first ? 4 : 12);
        }
        for (Paragraph p : parts) {
            doc.add(p);
        }
    }

    // ---- html -----------------------------------------------------------

    /**
     * The stored body, which is the sanitiser's allowlist and nothing else:
     * p, br, strong/b, em/i, u, s, ol, ul, li, h3, h4, blockquote, span, a.
     * <p>
     * Block elements become paragraphs and inline ones become styled chunks.
     * Anything unrecognised still contributes its text rather than vanishing -
     * losing a parent's homework to an unexpected tag is far worse than
     * rendering it unstyled.
     */
    private List<Paragraph> html(String bodyHtml, Fonts f) {
        List<Paragraph> out = new ArrayList<>();
        if (bodyHtml == null || bodyHtml.trim().isEmpty()) {
            return out;
        }
        org.jsoup.nodes.Document parsed = Jsoup.parseBodyFragment(bodyHtml);
        block(parsed.body(), f, out, new Style(), 0, null);
        return out;
    }

    /** The inline state inherited down the tree. */
    private static final class Style {
        private boolean bold;
        private boolean italic;
        private boolean underline;
        private boolean strike;

        private Style copy() {
            Style s = new Style();
            s.bold = bold;
            s.italic = italic;
            s.underline = underline;
            s.strike = strike;
            return s;
        }
    }

    /**
     * Walks block-level structure, flushing a paragraph whenever one closes.
     *
     * @param marker bullet or number to prefix, for list items
     */
    private void block(Node node, Fonts f, List<Paragraph> out, Style style,
                       int depth, String marker) {

        Paragraph current = new Paragraph();
        current.setLeading(14.5f);
        boolean wrote = false;

        for (Node child : node.childNodes()) {
            String tag = child.nodeName().toLowerCase();
            if (isBlock(tag)) {
                if (wrote) {
                    out.add(finish(current, depth, marker));
                    current = new Paragraph();
                    current.setLeading(14.5f);
                    wrote = false;
                }
                blockChild(child, tag, f, out, style, depth);
            } else {
                wrote |= inline(child, f, current, style);
            }
        }
        if (wrote) {
            out.add(finish(current, depth, marker));
        }
    }

    private void blockChild(Node child, String tag, Fonts f, List<Paragraph> out,
                            Style style, int depth) {
        switch (tag) {
            case "ul":
            case "ol":
                int n = 1;
                for (Node li : child.childNodes()) {
                    if (!"li".equals(li.nodeName().toLowerCase())) {
                        continue;
                    }
                    String bullet = "ol".equals(tag) ? (n++) + ". " : "• ";
                    block(li, f, out, style.copy(), depth + 1, bullet);
                }
                break;
            case "h3":
            case "h4": {
                Style heading = style.copy();
                heading.bold = true;
                block(child, f, out, heading, depth, null);
                break;
            }
            case "blockquote":
                block(child, f, out, style.copy(), depth + 1, null);
                break;
            default:
                block(child, f, out, style.copy(), depth, null);
        }
    }

    /**
     * @return true when anything was actually added, so empty paragraphs - which
     * an editor emits freely - do not become blank lines in the PDF
     */
    private boolean inline(Node node, Fonts f, Paragraph into, Style style) {
        String tag = node.nodeName().toLowerCase();

        if (node instanceof TextNode) {
            String text = ((TextNode) node).text();
            if (text.trim().isEmpty()) {
                // Whitespace between tags is still a word gap once the tags go.
                if (!text.isEmpty() && into.size() > 0) {
                    into.add(new Chunk(" ", fontFor(style, f)));
                }
                return false;
            }
            into.add(new Chunk(text, fontFor(style, f)));
            return true;
        }
        if ("br".equals(tag)) {
            into.add(Chunk.NEWLINE);
            return true;
        }

        Style next = style.copy();
        switch (tag) {
            case "strong":
            case "b":
                next.bold = true;
                break;
            case "em":
            case "i":
                next.italic = true;
                break;
            case "u":
                next.underline = true;
                break;
            case "s":
                next.strike = true;
                break;
            default:
                break;
        }

        boolean wrote = false;
        for (Node child : node.childNodes()) {
            wrote |= inline(child, f, into, next);
        }
        // An <a> prints its address too: on paper the href is the only way to
        // follow it, and in a PDF reader the text alone is not clickable.
        if ("a".equals(tag)) {
            String href = node.attr("href");
            if (href != null && !href.isEmpty()) {
                into.add(new Chunk(" (" + href + ")", f.small));
                wrote = true;
            }
        }
        return wrote;
    }

    private Font fontFor(Style s, Fonts f) {
        Font base = s.bold ? f.bodyBold : s.italic ? f.bodyItalic : f.body;
        if (!s.underline && !s.strike) {
            return base;
        }
        Font styled = new Font(base);
        styled.setStyle((s.underline ? Font.UNDERLINE : 0)
                | (s.strike ? Font.STRIKETHRU : 0));
        return styled;
    }

    private Paragraph finish(Paragraph p, int depth, String marker) {
        if (marker != null) {
            p.add(0, new Chunk(marker));
        }
        p.setIndentationLeft(10 + depth * 12);
        p.setSpacingBefore(2);
        return p;
    }

    private static boolean isBlock(String tag) {
        return "p".equals(tag) || "ul".equals(tag) || "ol".equals(tag)
                || "li".equals(tag) || "h3".equals(tag) || "h4".equals(tag)
                || "blockquote".equals(tag) || "div".equals(tag);
    }

    // ---- resources ------------------------------------------------------

    /** 2026-03-12 -> 12.03.2026, which is how the school writes a date. */
    private static String formatDate(String iso) {
        if (iso == null || iso.length() != 10) {
            return iso == null ? "" : iso;
        }
        return iso.substring(8, 10) + "." + iso.substring(5, 7) + "." + iso.substring(0, 4);
    }

    private Fonts fonts() throws SGSException {
        Fonts local = fonts;
        if (local == null) {
            synchronized (this) {
                local = fonts;
                if (local == null) {
                    fonts = local = new Fonts(load(FONT_REGULAR), load(FONT_BOLD));
                }
            }
        }
        return local;
    }

    /**
     * Embedded, and from the jar rather than the filesystem: the server has no
     * Georgian font installed and should not have to.
     */
    private BaseFont load(String resource) throws SGSException {
        try {
            byte[] bytes = read(resource);
            return BaseFont.createFont(resource, BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED, BaseFont.CACHED, bytes, null);
        } catch (DocumentException | IOException e) {
            throw new SGSException(SGSExceptionCode.INTERNAL_SEVER_ERROR,
                    "PDF-ის შრიფტი ვერ ჩაიტვირთა");
        }
    }

    private Image logo() {
        try {
            return Image.getInstance(read(LOGO));
        } catch (Exception e) {
            // A missing logo is not worth failing a download over.
            return null;
        }
    }

    private byte[] read(String resource) throws IOException {
        try (InputStream in = HomeworkPdfService.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("missing classpath resource " + resource);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }
}
