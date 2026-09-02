package mthiebi.sgs.gradebook.engine;

import mthiebi.sgs.gradebook.model.PeriodKind;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The reporting calendar, resolved once per scheme.
 * <p>
 * Two things depend on it: CHILDREN term resolution (YEAR.ANNUAL drawing on
 * T1/T2/T3), and recompute ordering - affected cells are sorted by depth
 * descending so leaf periods resolve before the rollups that consume them.
 */
public class PeriodTree {

    private final Map<Long, PeriodNode> byId;

    /**
     * @param nodes in calendar order - PeriodRepository.findByScheme sorts by
     *              depth then ordinal, and that order is preserved here.
     *              <p>
     *              A LinkedHashMap, deliberately. It was a HashMap, so periodsAtDepth and
     *              everything built on it - descendantsAtDepth, and therefore DESCENDANTS -
     *              returned periods in hash order while CHILDREN returned them in calendar
     *              order. Order is load-bearing: Evaluator resolves FIRST_NON_NULL as the
     *              first value and LATEST as the last, and FIRST_NON_NULL is the editor's
     *              default. A DESCENDANTS term with either reduction would have picked an
     *              arbitrary month.
     */
    public PeriodTree(List<PeriodNode> nodes) {
        this.byId = nodes.stream().collect(Collectors.toMap(
                PeriodNode::getId, Function.identity(),
                (a, b) -> a, java.util.LinkedHashMap::new));
    }

    public PeriodNode node(Long periodId) {
        PeriodNode node = byId.get(periodId);
        if (node == null) {
            throw new IllegalArgumentException("unknown period: " + periodId);
        }
        return node;
    }

    public List<Long> children(Long periodId) {
        PeriodNode node = byId.get(periodId);
        return node == null ? Collections.emptyList() : node.getChildIds();
    }

    public Long parent(Long periodId) {
        PeriodNode node = byId.get(periodId);
        return node == null ? null : node.getParentId();
    }

    public int depth(Long periodId) {
        PeriodNode node = byId.get(periodId);
        return node == null ? 0 : node.getDepth();
    }

    /**
     * The depth this kind of period sits at, or -1 when the scheme has none.
     * <p>
     * A component declares the kind of period its column lives on - REPORTING
     * for a reporting period, ROLLUP for a trimester, YEAR for the year - and
     * this turns that into the tier the engine works in.
     * <p>
     * The shallowest, if a kind ever appears at two depths. It did once: db/013
     * made both months and weeks REPORTING, so a monthly column totalling its
     * weeks was a legitimate rollup between two REPORTING tiers. The tree stops
     * at reporting periods now, but the ambiguity is answered rather than
     * assumed away.
     */
    public int depthOfKind(PeriodKind kind) {
        int found = -1;
        for (PeriodNode node : byId.values()) {
            if (node.getKind() == kind && (found < 0 || node.getDepth() < found)) {
                found = node.getDepth();
            }
        }
        return found;
    }

    /**
     * Every period sitting at the given tier, used to bound SPECIFIC term fan-out.
     */
    public List<Long> periodsOfKind(PeriodKind kind) {
        return byId.values().stream()
                .filter(n -> n.getKind() == kind)
                .map(PeriodNode::getId)
                .collect(Collectors.toList());
    }

    /**
     * The deepest level this scheme actually has.
     * <p>
     * Measured rather than declared. It was a {@code MAX_DEPTH = 3} constant
     * copied into two services, written when depth 3 held days. Reading it off
     * the tree means a scheme that gains or loses a level - as this one did when
     * the daily register stopped using periods - needs no edit anywhere.
     */
    public int maxDepth() {
        int deepest = 0;
        for (PeriodNode node : byId.values()) {
            if (node.getDepth() > deepest) {
                deepest = node.getDepth();
            }
        }
        return deepest;
    }

    /**
     * Every period at one level of the tree.
     */
    public List<Long> periodsAtDepth(int depth) {
        List<Long> result = new java.util.ArrayList<>();
        for (PeriodNode node : byId.values()) {
            if (node.getDepth() == depth) {
                result.add(node.getId());
            }
        }
        return result;
    }

    public List<Long> periodsAtDepthOf(PeriodKind kind, Long referencePeriodId) {
        if (kind == PeriodKind.YEAR) {
            return periodsOfKind(PeriodKind.YEAR);
        }
        int depth = depth(referencePeriodId);
        List<Long> result = new java.util.ArrayList<>();
        for (Long candidate : periodsOfKind(kind)) {
            if (depth(candidate) == depth) {
                result.add(candidate);
            }
        }
        return result;
    }

    /**
     * Everything beneath a period at one level, however far down.
     * <p>
     * What DESCENDANTS resolves to. Filtered by walking each candidate's
     * ancestors rather than descending, because the tree is small - a year is
     * about 230 nodes - and walking up is exact where descending needs a queue.
     */
    public List<Long> descendantsAtDepth(Long periodId, int depth) {
        List<Long> result = new java.util.ArrayList<>();
        for (Long candidate : periodsAtDepth(depth)) {
            if (ancestors(candidate).contains(periodId)) {
                result.add(candidate);
            }
        }
        return result;
    }

    public List<Long> ancestors(Long periodId) {
        java.util.ArrayList<Long> result = new java.util.ArrayList<>();
        Long current = parent(periodId);
        while (current != null) {
            result.add(current);
            current = parent(current);
        }
        return result;
    }
}
