package mthiebi.sgs.gradebook.service.content;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.gradebook.model.PostImage;
import mthiebi.sgs.gradebook.repository.PostImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

/**
 * News pictures: taking one in, and never storing what arrived.
 * <p>
 * The school's server is short of space - it is why homework attachments are
 * links rather than files. News is the exception, because a post without its
 * picture is a worse page, so the picture is paid for by shrinking it rather
 * than by refusing it.
 * <p>
 * **What is stored is always re-encoded.** The upload is decoded to pixels,
 * scaled to fit {@link #MAX_EDGE}, and written back out from those pixels. That
 * is what turns a 4 MB phone photo into roughly 200 KB, and it is also the only
 * validation worth having: a file that will not decode is not an image, and a
 * payload smuggled into a comment segment or a polyglot does not survive being
 * redrawn. The declared content type is never trusted.
 * <p>
 * No new dependency - ImageIO is in the JDK.
 */
@Service
public class ImageService {

    /**
     * Refused above this. A phone photo is 3-5 MB, so this is generous.
     */
    public static final int MAX_UPLOAD_BYTES = 2 * 1024 * 1024;

    /**
     * Long edge, in pixels. Ample for a news picture on any screen.
     */
    public static final int MAX_EDGE = 1600;

    @Autowired
    private PostImageRepository postImageRepository;

    @Transactional(rollbackFor = Exception.class)
    public PostImage store(byte[] uploaded, Long actorId) throws SGSException {
        if (uploaded == null || uploaded.length == 0) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "ფაილი ცარიელია");
        }
        if (uploaded.length > MAX_UPLOAD_BYTES) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "სურათი 2 მბ-ზე დიდია");
        }

        BufferedImage source = decode(uploaded);
        BufferedImage scaled = scaleToFit(source);

        // Alpha is kept as PNG; everything else becomes JPEG. Flattening a
        // transparent logo onto JPEG's opaque canvas turns its background black.
        boolean hasAlpha = scaled.getColorModel().hasAlpha();
        String format = hasAlpha ? "png" : "jpg";
        byte[] encoded = encode(scaled, format);

        PostImage image = new PostImage();
        image.setUuid(UUID.randomUUID().toString());
        image.setContentType(hasAlpha ? "image/png" : "image/jpeg");
        image.setBytes(encoded);
        image.setByteSize(encoded.length);
        image.setWidth(scaled.getWidth());
        image.setHeight(scaled.getHeight());
        image.setCreatedBy(actorId);
        image.setUpdatedBy(actorId);
        return postImageRepository.save(image);
    }

    @Transactional(readOnly = true)
    public PostImage get(String uuid) throws SGSException {
        return postImageRepository.findByUuid(uuid)
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST,
                        "სურათი ვერ მოიძებნა"));
    }

    /**
     * Decoding is the validation.
     * <p>
     * ImageIO returns null rather than throwing when nothing can read the
     * stream, which is the case that matters: a .exe renamed to .jpg reaches
     * here and must not be stored.
     */
    private BufferedImage decode(byte[] uploaded) throws SGSException {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(uploaded));
            if (image == null) {
                throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                        "ფაილი სურათი არ არის");
            }
            return image;
        } catch (SGSException e) {
            throw e;
        } catch (Exception e) {
            // A truncated or malformed image throws from deep inside a reader.
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "სურათი ვერ წაიკითხა");
        }
    }

    /**
     * Scaled only downwards. Enlarging a small picture would cost bytes and
     * gain nothing, so anything already within the limit is redrawn at its own
     * size - still re-encoded, because that is the point.
     */
    private BufferedImage scaleToFit(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int longEdge = Math.max(width, height);

        int targetWidth = width;
        int targetHeight = height;
        if (longEdge > MAX_EDGE) {
            double ratio = (double) MAX_EDGE / longEdge;
            targetWidth = Math.max(1, (int) Math.round(width * ratio));
            targetHeight = Math.max(1, (int) Math.round(height * ratio));
        }

        boolean hasAlpha = source.getColorModel().hasAlpha();
        BufferedImage target = new BufferedImage(targetWidth, targetHeight,
                hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);

        Graphics2D g = target.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            if (!hasAlpha) {
                // Drawing a transparent source onto an opaque canvas without
                // this leaves the uncovered area black rather than white.
                g.setColor(java.awt.Color.WHITE);
                g.fillRect(0, 0, targetWidth, targetHeight);
            }
            g.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g.dispose();
        }
        return target;
    }

    private byte[] encode(BufferedImage image, String format) throws SGSException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (!ImageIO.write(image, format, out)) {
                throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                        "სურათის შენახვა ვერ მოხერხდა");
            }
            return out.toByteArray();
        } catch (SGSException e) {
            throw e;
        } catch (Exception e) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "სურათის შენახვა ვერ მოხერხდა");
        }
    }
}
