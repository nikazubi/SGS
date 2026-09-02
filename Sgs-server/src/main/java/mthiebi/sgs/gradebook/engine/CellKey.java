package mthiebi.sgs.gradebook.engine;

import lombok.Value;

/**
 * Identifies one cell: (enrollment, subject, period, component).
 * <p>
 * subjectId is null for components that are not subject scoped - ethics,
 * absence, and student-wide aggregates such as rating.
 */
@Value
public class CellKey {

    Long enrollmentId;
    Long subjectId;
    Long periodId;
    Long componentId;

    public static CellKey of(Long enrollmentId, Long subjectId, Long periodId, Long componentId) {
        return new CellKey(enrollmentId, subjectId, periodId, componentId);
    }

    public CellKey withComponent(Long otherComponentId) {
        return new CellKey(enrollmentId, subjectId, periodId, otherComponentId);
    }

    public CellKey withPeriod(Long otherPeriodId) {
        return new CellKey(enrollmentId, subjectId, otherPeriodId, componentId);
    }

    public CellKey withSubject(Long otherSubjectId) {
        return new CellKey(enrollmentId, otherSubjectId, periodId, componentId);
    }
}
