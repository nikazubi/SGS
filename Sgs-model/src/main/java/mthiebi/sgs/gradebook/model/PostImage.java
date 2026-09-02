package mthiebi.sgs.gradebook.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * A news picture.
 * <p>
 * Its own table, not a column on {@link Post}: listing news would otherwise drag
 * every image's bytes through a query that only wants dates and titles. Served
 * by its own endpoint rather than base64 in JSON, so the browser can cache it.
 * <p>
 * What is stored is never what was uploaded. The upload is decoded, downscaled
 * to fit 1600px on its long edge, and written back out from the decoded pixels -
 * which is both how a 4 MB phone photo becomes about 200 KB and the security
 * check: a file that will not decode is not an image, and anything smuggled in
 * the original does not survive being redrawn.
 */
@Entity
@Table(name = "post_image", schema = "sgs",
        uniqueConstraints = @UniqueConstraint(name = "uq_post_image_uuid", columnNames = "uuid"))
@Getter
@Setter
public class PostImage extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "post_image_seq")
    @SequenceGenerator(name = "post_image_seq", sequenceName = "sgs.post_image_seq",
            allocationSize = 50)
    private Long id;

    /**
     * How the serving endpoint addresses it.
     */
    @Column(name = "uuid", nullable = false, length = 36)
    private String uuid;

    /**
     * What we wrote, not what was uploaded: image/jpeg or image/png.
     */
    @Column(name = "content_type", nullable = false, length = 64)
    private String contentType;

    @Column(name = "byte_size", nullable = false)
    private int byteSize;

    @Column(name = "width", nullable = false)
    private int width;

    @Column(name = "height", nullable = false)
    private int height;

    /**
     * Lazy, and the reason this is a separate table. Fetched only by the
     * endpoint that serves the file.
     */
    @Lob
    @Column(name = "bytes", nullable = false)
    private byte[] bytes;
}
