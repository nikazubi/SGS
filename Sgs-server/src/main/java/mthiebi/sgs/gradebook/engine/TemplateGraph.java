package mthiebi.sgs.gradebook.engine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The executable form of one template version: components indexed by id and
 * code, edges from each source component to the components that consume it,
 * and a topological order.
 * <p>
 * Built once per template version and cached - it is immutable for the lifetime
 * of that version, which is the point of versioning the template at all.
 */
public class TemplateGraph {

    private final Map<Long, ComponentDef> byId;
    private final Map<String, ComponentDef> byCode;
    /**
     * source component id -> components that read it
     */
    private final Map<Long, Set<Long>> dependents;
    /**
     * component id -> position in topological order
     */
    private final Map<Long, Integer> topoIndex;
    private final List<ComponentDef> ordered;

    private TemplateGraph(Map<Long, ComponentDef> byId,
                          Map<String, ComponentDef> byCode,
                          Map<Long, Set<Long>> dependents,
                          Map<Long, Integer> topoIndex,
                          List<ComponentDef> ordered) {
        this.byId = byId;
        this.byCode = byCode;
        this.dependents = dependents;
        this.topoIndex = topoIndex;
        this.ordered = ordered;
    }

    public static TemplateGraph build(List<ComponentDef> components) {
        Map<Long, ComponentDef> byId = new HashMap<>();
        Map<String, ComponentDef> byCode = new HashMap<>();
        for (ComponentDef c : components) {
            if (byId.put(c.getId(), c) != null) {
                throw new TemplateGraphException("duplicate component id: " + c.getId());
            }
            if (byCode.put(c.getCode(), c) != null) {
                throw new TemplateGraphException("duplicate component code: " + c.getCode());
            }
        }

        Map<Long, Set<Long>> dependents = new HashMap<>();
        Map<Long, Integer> inDegree = new HashMap<>();
        for (ComponentDef c : components) {
            inDegree.putIfAbsent(c.getId(), 0);
        }

        for (ComponentDef c : components) {
            if (!c.isDerived()) {
                continue;
            }
            // A component may read the same source more than once (several terms
            // over one column); the edge is still a single dependency.
            Set<Long> sources = new LinkedHashSet<>(c.getRule().allSourceComponentIds());
            for (Long sourceId : sources) {
                if (!byId.containsKey(sourceId)) {
                    throw new TemplateGraphException(
                            "component " + c.getCode() + " references unknown component id " + sourceId);
                }
                if (sourceId.equals(c.getId())) {
                    throw new TemplateGraphException("component " + c.getCode() + " references itself");
                }
                dependents.computeIfAbsent(sourceId, k -> new LinkedHashSet<>()).add(c.getId());
                inDegree.merge(c.getId(), 1, Integer::sum);
            }
        }

        List<ComponentDef> ordered = topologicalOrder(components, byId, dependents, inDegree);

        Map<Long, Integer> topoIndex = new HashMap<>();
        for (int i = 0; i < ordered.size(); i++) {
            topoIndex.put(ordered.get(i).getId(), i);
        }

        return new TemplateGraph(byId, byCode, dependents, topoIndex, ordered);
    }

    private static List<ComponentDef> topologicalOrder(List<ComponentDef> components,
                                                       Map<Long, ComponentDef> byId,
                                                       Map<Long, Set<Long>> dependents,
                                                       Map<Long, Integer> inDegree) {
        Map<Long, Integer> remaining = new HashMap<>(inDegree);
        Deque<Long> ready = new ArrayDeque<>();
        // Seed in declared order so the result is stable between builds.
        for (ComponentDef c : components) {
            if (remaining.getOrDefault(c.getId(), 0) == 0) {
                ready.add(c.getId());
            }
        }

        List<ComponentDef> ordered = new ArrayList<>(components.size());
        while (!ready.isEmpty()) {
            Long id = ready.poll();
            ordered.add(byId.get(id));
            for (Long dependent : dependents.getOrDefault(id, Collections.emptySet())) {
                int left = remaining.merge(dependent, -1, Integer::sum);
                if (left == 0) {
                    ready.add(dependent);
                }
            }
        }

        if (ordered.size() != components.size()) {
            Set<String> stuck = new HashSet<>();
            for (ComponentDef c : components) {
                if (remaining.getOrDefault(c.getId(), 0) > 0) {
                    stuck.add(c.getCode());
                }
            }
            throw new TemplateGraphException("derivation cycle involving: " + stuck);
        }
        return ordered;
    }

    public ComponentDef byId(Long componentId) {
        ComponentDef c = byId.get(componentId);
        if (c == null) {
            throw new TemplateGraphException("unknown component id: " + componentId);
        }
        return c;
    }

    public ComponentDef byCode(String code) {
        return byCode.get(code);
    }

    public boolean hasCode(String code) {
        return byCode.containsKey(code);
    }

    /**
     * Components that read the given one, directly.
     */
    public Set<Long> dependentsOf(Long componentId) {
        return dependents.getOrDefault(componentId, Collections.emptySet());
    }

    public int topoIndexOf(Long componentId) {
        Integer index = topoIndex.get(componentId);
        return index == null ? Integer.MAX_VALUE : index;
    }

    public List<ComponentDef> inTopologicalOrder() {
        return Collections.unmodifiableList(ordered);
    }

    public List<ComponentDef> all() {
        return Collections.unmodifiableList(ordered);
    }
}
