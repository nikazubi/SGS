package mthiebi.sgs.gradebook.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.math.BigDecimal;

/**
 * The one formula that turns a stored mark into the printed one.
 * <p>
 * The school grades on one scale and reports on another - today a hardcoded
 * "+3", an IB 7-point to Georgian 10-point conversion, buried in two
 * copy-pasted export methods behind a checkbox. It has changed repeatedly over
 * the years, so it becomes something the school edits.
 * <p>
 * **Display only.** No grade is ever stored converted, nothing recomputes
 * through it, and the parent portal does not use it. It applies in exactly two
 * places: the grid, when someone turns the toggle on, and the Excel export,
 * when they tick the box. Editing it therefore cannot corrupt anything - the
 * next render simply reads differently.
 * <p>
 * One row. There is one formula at a time, not one per journal or per column:
 * a journal that should not be converted is simply never viewed with the toggle
 * on. Scoping it per column was considered and dropped as machinery the school
 * did not ask for.
 */
@Entity
@Table(name = "conversion_formula", schema = "sgs")
@Getter
@Setter
public class ConversionFormula extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "conversion_formula_seq")
    @SequenceGenerator(name = "conversion_formula_seq", sequenceName = "sgs.conversion_formula_seq",
            allocationSize = 50)
    private Long id;

    /**
     * What the school calls the printed scale, e.g. "ათბალიანი". Shown on the toggle.
     */
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    /**
     * Null reads as 1, so an offset-only formula needs one number.
     */
    @Column(name = "multiplier", precision = 9, scale = 4)
    private BigDecimal multiplier;

    /**
     * Null reads as 0. The legacy conversion is multiplier 1, offset 3.
     */
    @Column(name = "offset_value", precision = 9, scale = 4)
    private BigDecimal offsetValue;
}
