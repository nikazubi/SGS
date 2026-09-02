package mthiebi.sgs.gradebook.engine;

import mthiebi.sgs.gradebook.model.NullPolicy;
import mthiebi.sgs.gradebook.model.PeriodKind;
import mthiebi.sgs.gradebook.model.PeriodRef;
import mthiebi.sgs.gradebook.model.ReduceType;
import mthiebi.sgs.gradebook.model.RuleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static mthiebi.sgs.gradebook.engine.TemplateFixture.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * These conditions are rejected when a template version is saved, which is why
 * the evaluator has no error path for bad configuration - a teacher entering
 * marks can never be shown one.
 */
class TemplateGraphTest {

    private RuleDef readsOf(Long... sources) {
        return rule(RuleType.AVERAGE, NullPolicy.IGNORE, false, RoundingMode.HALF_UP, 2,
                Collections.singletonList(
                        group(ReduceType.AVERAGE, PeriodRef.SAME, Arrays.asList(sources), "reads")),
                null);
    }

    @Test
    @DisplayName("a derivation cycle is rejected, naming the components involved")
    void rejectsCycles() {
        List<ComponentDef> components = Arrays.asList(
                derived(1L, "A", PeriodKind.ROLLUP, true, readsOf(2L)),
                derived(2L, "B", PeriodKind.ROLLUP, true, readsOf(3L)),
                derived(3L, "C", PeriodKind.ROLLUP, true, readsOf(1L)));

        TemplateGraphException error =
                assertThrows(TemplateGraphException.class, () -> TemplateGraph.build(components));
        assertTrue(error.getMessage().contains("cycle"), error.getMessage());
        assertTrue(error.getMessage().contains("A"), error.getMessage());
    }

    @Test
    @DisplayName("a component referencing one that does not exist is rejected")
    void rejectsDanglingReferences() {
        List<ComponentDef> components = Collections.singletonList(
                derived(1L, "A", PeriodKind.ROLLUP, true, readsOf(999L)));

        TemplateGraphException error =
                assertThrows(TemplateGraphException.class, () -> TemplateGraph.build(components));
        assertTrue(error.getMessage().contains("999"), error.getMessage());
    }

    @Test
    @DisplayName("a component referencing itself is rejected")
    void rejectsSelfReference() {
        List<ComponentDef> components = Collections.singletonList(
                derived(1L, "A", PeriodKind.ROLLUP, true, readsOf(1L)));

        TemplateGraphException error =
                assertThrows(TemplateGraphException.class, () -> TemplateGraph.build(components));
        assertTrue(error.getMessage().contains("itself"), error.getMessage());
    }

    @Test
    @DisplayName("duplicate codes are rejected")
    void rejectsDuplicateCodes() {
        List<ComponentDef> components = Arrays.asList(
                input(1L, "SAME", PeriodKind.ROLLUP, true),
                input(2L, "SAME", PeriodKind.ROLLUP, true));

        TemplateGraphException error =
                assertThrows(TemplateGraphException.class, () -> TemplateGraph.build(components));
        assertTrue(error.getMessage().contains("duplicate component code"), error.getMessage());
    }

    @Test
    @DisplayName("components sort so that every source precedes what reads it")
    void producesTopologicalOrder() {
        TemplateGraph graph = graph();
        int avg = graph.topoIndexOf(ONGOING_AVG);
        int trimester = graph.topoIndexOf(TRIMESTER_GRADE);
        int annual = graph.topoIndexOf(ANNUAL);

        assertTrue(graph.topoIndexOf(ONGOING_1) < avg);
        assertTrue(avg < trimester);
        assertTrue(trimester < annual);
    }

    @Test
    @DisplayName("dependents are found in both directions of the graph")
    void tracksDependents() {
        TemplateGraph graph = graph();
        assertTrue(graph.dependentsOf(ONGOING_1).contains(ONGOING_AVG));
        assertTrue(graph.dependentsOf(ONGOING_AVG).contains(TRIMESTER_GRADE));
        assertTrue(graph.dependentsOf(TRIMESTER_GRADE).contains(ANNUAL));
        assertTrue(graph.dependentsOf(TRIMESTER_GRADE).contains(RATING));
        assertTrue(graph.dependentsOf(ANNUAL).isEmpty());
    }

    @Test
    @DisplayName("components read only by a fallback rule still create an edge")
    void followsFallbackChainsWhenBuildingEdges() {
        // Easy to miss: if fallback sources are not walked, editing the resit
        // mark would never trigger a recompute of the column that falls back
        // onto it.
        RuleDef fallback = readsOf(2L);
        RuleDef primary = rule(RuleType.AVERAGE, NullPolicy.BLOCK, false, RoundingMode.HALF_UP, 2,
                Collections.singletonList(
                        group(ReduceType.AVERAGE, PeriodRef.SAME, Collections.singletonList(1L), "primary")),
                fallback);

        TemplateGraph graph = TemplateGraph.build(Arrays.asList(
                input(1L, "PRIMARY", PeriodKind.ROLLUP, true),
                input(2L, "RESIT", PeriodKind.ROLLUP, true),
                derived(3L, "SUMMARY", PeriodKind.ROLLUP, true, primary)));

        assertTrue(graph.dependentsOf(2L).contains(3L),
                "the fallback's source must be an edge, or its edits never propagate");
    }
}
