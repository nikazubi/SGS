package mthiebi.sgs.gradebook.service.grid;

import lombok.Value;
import mthiebi.sgs.gradebook.model.GradeComponent;
import mthiebi.sgs.gradebook.model.GradeEntry;
import mthiebi.sgs.gradebook.model.Period;
import mthiebi.sgs.gradebook.model.TemplateVersion;
import mthiebi.sgs.gradebook.repository.GradeComponentRepository;
import mthiebi.sgs.gradebook.repository.GradeEntryRepository;
import mthiebi.sgs.gradebook.repository.PeriodRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The marks from the periods underneath the one on screen, for reading beside
 * it.
 * <p>
 * On the year, the brief's table puts the three trimester assessments next to
 * the annual one, the final exam and the overall grade - the annual grade is
 * their average, and a teacher typing the exam should be able to see what it
 * is being averaged with. The year screen showed only the year's own columns.
 * <p>
 * Which columns: every one marked as a summary ({@code component.summary_column},
 * db/034) that lives on a lower tier than the period being viewed, once per
 * period of that tier inside it - the trimester assessment, three times. The
 * parent's year view draws the same breakdown from the same flag.
 * <p>
 * <b>Read-only, and only ever read.</b> A column here is a (component, period)
 * pair, and nothing that writes - the batch save, recomputation, change
 * requests, publication - knows about that shape. Making them editable would
 * change how every cell is identified; displaying them changes nothing.
 */
@Component
public class SummaryBreakdown {

    @Autowired
    private GradeComponentRepository componentRepository;

    @Autowired
    private PeriodRepository periodRepository;

    @Autowired
    private GradeEntryRepository gradeEntryRepository;

    /**
     * In calendar order within each component: the first trimester first.
     */
    public List<Column> columnsFor(TemplateVersion version, Period at, boolean subjectScoped) {
        List<GradeComponent> marked = componentRepository.findByTemplateVersion(version.getId())
                .stream()
                .filter(GradeComponent::isSummaryColumn)
                .filter(c -> c.isSubjectScoped() == subjectScoped)
                .filter(c -> c.getPeriodKind() != at.getKind())
                .collect(Collectors.toList());
        if (marked.isEmpty()) {
            return Collections.emptyList();
        }

        // Ordered by depth, then ordinal - which is calendar order on one tier.
        List<Period> scheme = periodRepository.findByScheme(at.getScheme().getId());
        Map<Long, Period> byId = scheme.stream()
                .collect(Collectors.toMap(Period::getId, p -> p));

        List<Column> columns = new ArrayList<>();
        for (GradeComponent component : marked) {
            for (Period period : scheme) {
                if (period.getKind() == component.getPeriodKind() && below(period, at, byId)) {
                    columns.add(new Column(component, period));
                }
            }
        }
        return columns;
    }

    /**
     * Each borrowed cell, keyed {@code enrollmentId:columnCode}.
     * <p>
     * Matched by the component's code within the journal, not by its id: a
     * trimester can be pinned to an older version of the journal than the year
     * (a version holding marks is never edited in place), and there the same
     * column has a different id.
     */
    public Map<String, GradeEntry> valuesFor(List<Column> columns, Collection<Long> enrollmentIds,
                                             Long subjectId, Long journalId) {
        if (columns.isEmpty() || enrollmentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> periodIds = columns.stream()
                .map(c -> c.getPeriod().getId()).collect(Collectors.toSet());
        Set<String> wanted = columns.stream()
                .map(Column::getCode).collect(Collectors.toSet());

        Map<String, GradeEntry> values = new HashMap<>();
        for (GradeEntry entry : gradeEntryRepository.loadGrid(enrollmentIds, periodIds, subjectId)) {
            // loadGrid also returns the subject-less cells beside a subject's.
            if (subjectId != null && entry.getSubject() == null) {
                continue;
            }
            if (!entry.getTemplateVersion().getTemplate().getId().equals(journalId)) {
                continue;
            }
            String code = Column.codeOf(entry.getComponent().getCode(), entry.getPeriod().getId());
            if (wanted.contains(code)) {
                values.put(entry.getEnrollment().getId() + ":" + code, entry);
            }
        }
        return values;
    }

    /**
     * Strictly underneath: a period is not below itself.
     */
    private boolean below(Period period, Period ancestor, Map<Long, Period> byId) {
        Period walk = period.getParent() == null ? null : byId.get(period.getParent().getId());
        while (walk != null) {
            if (walk.getId().equals(ancestor.getId())) {
                return true;
            }
            walk = walk.getParent() == null ? null : byId.get(walk.getParent().getId());
        }
        return false;
    }

    /**
     * One borrowed column: a component, read at a period below the one shown.
     */
    @Value
    public static class Column {
        GradeComponent component;
        Period period;

        static String codeOf(String componentCode, Long periodId) {
            return componentCode + "@" + periodId;
        }

        /**
         * Distinct from every real column code, which is what the screen keys
         * cells by: the trimester assessment appears three times.
         */
        public String getCode() {
            return codeOf(component.getCode(), period.getId());
        }
    }
}
