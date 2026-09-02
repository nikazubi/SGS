package mthiebi.sgs.gradebook.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

/**
 * A component feeding a term: one row for a COMPONENT term, many for a GROUP.
 * A real foreign key, which is what keeps this from degenerating into
 * stringly-typed EAV - a source cannot reference a column that does not exist
 * in its template version.
 */
@Entity
@Table(name = "derivation_source", schema = "sgs",
        uniqueConstraints = @UniqueConstraint(name = "uq_source_term_component",
                columnNames = {"term_id", "component_id"}),
        indexes = @Index(name = "ix_source_component", columnList = "component_id"))
@Getter
@Setter
public class DerivationSource {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "derivation_source_seq")
    @SequenceGenerator(name = "derivation_source_seq", sequenceName = "sgs.derivation_source_seq",
            allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_source_term"))
    private DerivationTerm term;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "component_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_source_component"))
    private GradeComponent component;
}
