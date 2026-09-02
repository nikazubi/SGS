package mthiebi.sgs.gradebook.service.grid;

import lombok.AllArgsConstructor;
import lombok.Data;
import mthiebi.sgs.gradebook.model.GradeSource;

import java.math.BigDecimal;

/**
 * One cell as it currently stands.
 * <p>
 * Only cells that exist are sent; a blank cell is simply absent, which keeps a
 * mostly-empty early-term grid small.
 */
@Data
@AllArgsConstructor
public class GridCell {

    /**
     * The row id. Needed because a change request points at the cell itself,
     * not at its coordinates - so a locked cell cannot be disputed without it.
     */
    private Long id;

    private Long enrollmentId;
    private String componentCode;

    private BigDecimal value;
    private String specialValue;

    private GradeSource source;

    /**
     * A calculated cell typed over by a person. Sticky until explicitly cleared.
     */
    private boolean override;

    /**
     * What the client must send back as {@code expectedVersion} to write this
     * cell safely. Without it the console cannot detect a competing edit.
     */
    private int rowVersion;

    /**
     * Published to parents, so it cannot be edited directly - only through a
     * change request. Recomputation is unaffected.
     */
    private boolean published;

    /**
     * What parents were shown. Needed by the change-request dialog, which is
     * about to ask a director to move away from exactly this number - showing
     * the working value there would hide the divergence the flow exists for.
     */
    private BigDecimal publishedValue;
    private String publishedSpecialValue;

    /**
     * Published, and the working value has since moved away from what parents
     * were shown. Always false until something is actually published.
     */
    private boolean changedSincePublication;

    /**
     * A request is already waiting on the director. Shown so a teacher does not
     * raise a second one; the filtered unique index is what actually prevents
     * it, this is so they are told before they type.
     */
    private boolean changeRequestPending;

    /**
     * The value as the school's conversion formula prints it, or null when no
     * formula is configured.
     * <p>
     * Sent whether or not the console is currently showing converted values, so
     * the toggle is instant and needs no refetch.
     * <p>
     * Deliberately a separate field rather than a converted {@code value}: the
     * write path reads {@code value} and has no route to this one, so no bug in
     * the console can send a converted number back as a grade. The scaleMin /
     * scaleMax check would reject it anyway, which is the second of the two
     * guards behind "a converted grid is read-only".
     */
    private BigDecimal convertedValue;
}
