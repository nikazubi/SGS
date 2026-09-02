package mthiebi.sgs.gradebook.service;

import mthiebi.sgs.gradebook.engine.PeriodNode;
import mthiebi.sgs.gradebook.engine.PeriodTree;
import mthiebi.sgs.gradebook.model.Period;
import mthiebi.sgs.gradebook.repository.PeriodRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads a reporting calendar once per scheme and keeps it. A scheme belongs to
 * an academic year and does not change during it.
 */
@Service
public class PeriodTreeLoader {

    private final Map<Long, PeriodTree> cache = new ConcurrentHashMap<>();

    @Autowired
    private PeriodRepository periodRepository;

    /**
     * Cached for the life of the process, so a period added mid-year - the
     * documented way to give a long month its fifth and sixth week - would
     * otherwise stay invisible until a restart.
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 300000)
    public void refresh() {
        cache.clear();
    }

    public PeriodTree treeOf(Long schemeId) {
        return cache.computeIfAbsent(schemeId, this::load);
    }

    public void evict(Long schemeId) {
        cache.remove(schemeId);
    }

    @Transactional(readOnly = true)
    protected PeriodTree load(Long schemeId) {
        List<Period> periods = periodRepository.findByScheme(schemeId);

        Map<Long, List<Long>> childrenByParent = new LinkedHashMap<>();
        for (Period period : periods) {
            Long parentId = period.getParent() == null ? null : period.getParent().getId();
            if (parentId != null) {
                childrenByParent.computeIfAbsent(parentId, k -> new ArrayList<>()).add(period.getId());
            }
        }

        List<PeriodNode> nodes = new ArrayList<>(periods.size());
        for (Period period : periods) {
            nodes.add(new PeriodNode(
                    period.getId(),
                    period.getParent() == null ? null : period.getParent().getId(),
                    period.getCode(),
                    period.getDepth(),
                    period.getKind(),
                    childrenByParent.getOrDefault(period.getId(), Collections.emptyList())));
        }
        return new PeriodTree(nodes);
    }
}
