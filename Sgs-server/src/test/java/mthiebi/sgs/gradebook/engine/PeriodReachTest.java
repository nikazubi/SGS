package mthiebi.sgs.gradebook.engine;

import mthiebi.sgs.gradebook.model.ComponentKind;
import mthiebi.sgs.gradebook.model.NullPolicy;
import mthiebi.sgs.gradebook.model.PeriodKind;
import mthiebi.sgs.gradebook.model.PeriodRef;
import mthiebi.sgs.gradebook.model.ReduceType;
import mthiebi.sgs.gradebook.model.RuleType;
import mthiebi.sgs.gradebook.model.SourceKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The reach, and the property nothing used to assert.
 * <p>
 * Six services each answered "which periods does this touch?" in their own
 * inline expression. They had to agree; nothing checked that they did, and they
 * did not - only some of them learned about DESCENDANTS, so a yearly total was
 * computed from one month of data and stored over the year, and the explain
 * trace reported sources the write path had summed as empty.
 * <p>
 * The test that matters here is {@link #sourcesAndDependentsAreInverses}: for
 * every reference shape, if evaluating a cell at X reads period P, then a change
 * at P must recompute X. That is the invariant the two halves of the engine have
 * to satisfy, and it was expressible only once one class owned both.
 */
class PeriodReachTest {

    private static final Long YEAR = 200L;
    private static final Long T1 = 201L;
    private static final Long T2 = 202L;
    private static final Long SEP = 211L;
    private static final Long OCT = 212L;
    private static final Long FEB = 221L;

    /**
     * The real shape after the extraction: year, trimesters, months. Depth stops at 2.
     */
    private PeriodReach reach() {
        return PeriodReach.of(new PeriodTree(Arrays.asList(
                new PeriodNode(YEAR, null, "YEAR", 0, PeriodKind.YEAR, Arrays.asList(T1, T2)),
                new PeriodNode(T1, YEAR, "T1", 1, PeriodKind.ROLLUP, Arrays.asList(SEP, OCT)),
                new PeriodNode(T2, YEAR, "T2", 1, PeriodKind.ROLLUP,
                        Collections.singletonList(FEB)),
                new PeriodNode(SEP, T1, "SEP", 2, PeriodKind.REPORTING, Collections.emptyList()),
                new PeriodNode(OCT, T1, "OCT", 2, PeriodKind.REPORTING, Collections.emptyList()),
                new PeriodNode(FEB, T2, "FEB", 2, PeriodKind.REPORTING,
                        Collections.emptyList()))));
    }

    // ---- the reaches themselves --------------------------------------------

    @Test
    @DisplayName("CHILDREN is exactly one level")
    void childrenIsOneLevel() {
        List<Long> sources = reach().sources(PeriodRef.CHILDREN, YEAR, 2, null);
        assertEquals(Arrays.asList(T1, T2), sources,
                "the year's children are trimesters, not the months beneath them");
    }

    @Test
    @DisplayName("DESCENDANTS spans the gap CHILDREN cannot")
    void descendantsSpansAnyDistance() {
        List<Long> sources = reach().sources(PeriodRef.DESCENDANTS, YEAR, 2, null);
        assertEquals(3, sources.size());
        assertTrue(sources.containsAll(Arrays.asList(SEP, OCT, FEB)),
                "every month, under either trimester");
    }

    @Test
    @DisplayName("DESCENDANTS from a trimester stays inside it")
    void descendantsIsBoundedByItsAncestor() {
        List<Long> sources = reach().sources(PeriodRef.DESCENDANTS, T1, 2, null);
        assertEquals(Arrays.asList(SEP, OCT), sources, "February belongs to the other trimester");
    }

    @Test
    @DisplayName("a subtree is the period and everything under it")
    void subtreeIsWhatPublicationReleases() {
        List<Long> all = reach().subtree(T1);
        assertEquals(3, all.size());
        assertTrue(all.containsAll(Arrays.asList(T1, SEP, OCT)));
    }

    @Test
    @DisplayName("the deepest level is measured, not declared")
    void maxDepthComesFromTheTree() {
        // It was a MAX_DEPTH = 3 constant in two services, written when depth 3
        // held days. Days are gone; a hardcoded 3 would still be looking for
        // them, and a scheme that grew a level would silently not be walked.
        assertEquals(2, reach().tree().maxDepth());
        assertEquals(6, reach().subtree(YEAR).size(),
                "the year, two trimesters and three months - the whole scheme");
    }

    // ---- the invariant -----------------------------------------------------

    @Test
    @DisplayName("if evaluating X reads P, then changing P recomputes X - and only X")
    void sourcesAndDependentsAreInverses() {
        PeriodReach reach = reach();

        // A yearly rollup over months, which is where the two directions came
        // apart: the evaluator reached two levels down, and the recompute side
        // resolved the inverse as the *parent* - so a September mark rewrote the
        // trimester, a level the yearly column does not live at.
        assertInverse(reach, PeriodRef.DESCENDANTS, YEAR, 2, 0);

        // And the case CHILDREN was always right for.
        assertInverse(reach, PeriodRef.CHILDREN, YEAR, 1, 0);
        assertInverse(reach, PeriodRef.CHILDREN, T1, 2, 1);

        // SAME is its own inverse.
        assertInverse(reach, PeriodRef.SAME, SEP, 2, 2);

        // SPECIFIC, which the earlier version of this test could not reach at
        // all: it passed a null specificId, so sources came back empty and the
        // loop body never ran.
        assertInverse(reach, PeriodRef.SPECIFIC, YEAR, 2, 0, SEP);
    }

    @Test
    @DisplayName("a YEAR column's dependents are narrowed to the root")
    void yearColumnsOnlyLiveAtTheRoot() {
        PeriodReach reach = reach();

        // The shape alone resolves CHILDREN to the parent, and September's
        // parent is the trimester. For a YEAR column that is a row at a level
        // the column does not exist at - invisible to every reader, and exactly
        // what db/023 exists to delete. RecomputeEngine applies this filter,
        // because only it knows the component's kind.
        assertEquals(Collections.singletonList(T1),
                reach.dependents(PeriodRef.CHILDREN, SEP, 1, null));
        assertTrue(reach.onlyAtRoot(reach.dependents(PeriodRef.CHILDREN, SEP, 1, null)).isEmpty(),
                "the trimester is not the root");
        assertEquals(Collections.singletonList(YEAR),
                reach.onlyAtRoot(reach.dependents(PeriodRef.CHILDREN, T1, 0, null)));
    }

    @Test
    @DisplayName("DESCENDANTS comes back in calendar order, like CHILDREN")
    void reachIsOrdered() {
        // periodsAtDepth iterated a HashMap, so DESCENDANTS returned months in
        // hash order while CHILDREN returned trimesters in calendar order. That
        // is load-bearing: Evaluator reads FIRST_NON_NULL as the first value and
        // LATEST as the last, and FIRST_NON_NULL is the editor default - so
        // either reduction over a DESCENDANTS term picked an arbitrary month.
        assertEquals(Arrays.asList(SEP, OCT, FEB),
                reach().sources(PeriodRef.DESCENDANTS, YEAR, 2, null));
        assertEquals(Arrays.asList(T1, T2),
                reach().sources(PeriodRef.CHILDREN, YEAR, 1, null));
    }

    private void assertInverse(PeriodReach reach, PeriodRef ref, Long dependentPeriod,
                               int journalDepth, int dependentDepth) {
        assertInverse(reach, ref, dependentPeriod, journalDepth, dependentDepth, null);
    }

    /**
     * Both directions, for one term shape.
     * <p>
     * Forwards: everything the dependent reads must recompute it. Backwards:
     * everything that claims to recompute the dependent must actually be read by
     * it - the half the earlier version of this test omitted, and the half that
     * catches a reach resolving to a level the column does not live at.
     *
     * @param dependentPeriod the cell being evaluated
     * @param journalDepth    the level its sources are filled in at
     * @param dependentDepth  the level the dependent column itself lives at
     */
    private void assertInverse(PeriodReach reach, PeriodRef ref, Long dependentPeriod,
                               int journalDepth, int dependentDepth, Long specificId) {
        List<Long> sources = reach.sources(ref, dependentPeriod, journalDepth, specificId);
        assertFalse(sources.isEmpty(), ref + ": nothing is read, so the case is vacuous");

        for (Long source : sources) {
            List<Long> dependents = reach.dependents(ref, source, dependentDepth, specificId);
            assertTrue(dependents.contains(dependentPeriod),
                    ref + ": evaluating " + dependentPeriod + " reads " + source
                            + ", but changing " + source + " recomputes " + dependents
                            + " - the two directions disagree");

            for (Long dependent : dependents) {
                assertTrue(reach.sources(ref, dependent, journalDepth, specificId).contains(source),
                        ref + ": changing " + source + " recomputes " + dependent
                                + ", which does not read it - a write at a level"
                                + " nothing reads from");
            }
        }
    }

    // ---- the working set ---------------------------------------------------

    @Test
    @DisplayName("a graph with no DESCENDANTS loads only its neighbourhood")
    void workingSetStaysSmallWhenItCan() {
        // The subtree walk is not free, and the trimester journal never needed
        // it. Paid for only by a graph that reaches that far.
        List<Long> loaded = reach().workingSet(SEP, graph(PeriodRef.CHILDREN));
        assertTrue(loaded.contains(SEP));
        assertTrue(loaded.contains(T1));
        assertTrue(loaded.contains(YEAR));
        assertTrue(loaded.contains(OCT), "a sibling, through the ancestor's children");
        assertFalse(loaded.contains(FEB), "the other trimester's month is out of reach");
    }

    @Test
    @DisplayName("a DESCENDANTS graph loads every period its sum can reach")
    void workingSetWidensForDescendants() {
        // The bug this prevents: evaluating a yearly sum against a working set
        // holding one month computes the total from that month and stores it.
        // Nothing fails; the year is simply wrong from then on.
        List<Long> loaded = reach().workingSet(OCT, graph(PeriodRef.DESCENDANTS));
        assertTrue(loaded.containsAll(Arrays.asList(YEAR, T1, T2, SEP, OCT, FEB)),
                "October's write has to see September, or the year's total loses it");
    }

    // ---- fixture -----------------------------------------------------------

    /**
     * A yearly total over months, reached the given way.
     */
    private TemplateGraph graph(PeriodRef ref) {
        ComponentDef hours = new ComponentDef(1L, "HOURS_MISSED", "გაცდენა", 0,
                ComponentKind.INPUT, PeriodKind.ROLLUP, false, false, 0,
                java.math.BigDecimal.ZERO, new java.math.BigDecimal("500"), false, null);

        RuleDef rule = new RuleDef(RuleType.SUM, NullPolicy.IGNORE, false,
                java.math.RoundingMode.HALF_UP, 0,
                Collections.singletonList(new TermDef(0, java.math.BigDecimal.ONE,
                        SourceKind.COMPONENT, ReduceType.SUM, ref, null,
                        Collections.singletonList(1L), "თვები")),
                null);

        ComponentDef total = new ComponentDef(2L, "HOURS_YEAR", "ჯამი", 1,
                ComponentKind.DERIVED, PeriodKind.YEAR, false, true, 0,
                null, null, false, rule);

        return TemplateGraph.build(Arrays.asList(hours, total));
    }

}
