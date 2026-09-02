package mthiebi.sgs.gradebook.engine;

import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Everything the evaluator needs besides the cells themselves.
 * <p>
 * subjectIds is the set the student actually takes, and exists for ALL_SUBJECTS
 * terms - the cross-subject shape the legacy "rating" column needs and that no
 * within-subject rule can express.
 */
@Getter
public class EvaluationContext {

    private final TemplateGraph graph;
    private final PeriodTree periodTree;
    private final WorkingSet workingSet;
    private final List<Long> subjectIds;
    private final Map<String, SpecialValueBehaviour> specialBehaviours;
    /**
     * The depth of the tree this journal is filled in at.
     * <p>
     * Needed because a SPECIFIC term deliberately reads a journal at a
     * different frequency, so the changed period's own depth says nothing about
     * where the dependent's cells live.
     */
    private int journalDepth;

    public EvaluationContext(TemplateGraph graph,
                             PeriodTree periodTree,
                             WorkingSet workingSet,
                             List<Long> subjectIds,
                             Map<String, SpecialValueBehaviour> specialBehaviours) {
        this.graph = graph;
        this.periodTree = periodTree;
        this.workingSet = workingSet;
        this.subjectIds = subjectIds == null ? Collections.emptyList() : subjectIds;
        this.specialBehaviours = specialBehaviours == null ? Collections.emptyMap() : specialBehaviours;
    }

    public int getJournalDepth() {
        return journalDepth;
    }

    public void setJournalDepth(int journalDepth) {
        this.journalDepth = journalDepth;
    }

    public SpecialValueBehaviour behaviourOf(String specialCode) {
        return specialBehaviours.getOrDefault(specialCode, SpecialValueBehaviour.EXCLUDE);
    }
}
