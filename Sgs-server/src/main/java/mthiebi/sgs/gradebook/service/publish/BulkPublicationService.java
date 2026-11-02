package mthiebi.sgs.gradebook.service.publish;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.gradebook.model.GradingTemplate;
import mthiebi.sgs.gradebook.model.Period;
import mthiebi.sgs.gradebook.repository.GradingTemplateRepository;
import mthiebi.sgs.gradebook.repository.PeriodRepository;
import mthiebi.sgs.gradebook.service.grid.ClassGroupOption;
import mthiebi.sgs.gradebook.service.grid.GradebookLookupService;
import mthiebi.sgs.gradebook.service.grid.PeriodOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Releasing several classes and journals in one go.
 * <p>
 * The director's round used to be one class, one journal, one period at a
 * time - about 126 publish-and-confirm rounds per trimester for 42 classes and
 * three journals, each emailing that class's families. This is the same release,
 * {@link PublicationService#publish}, run for every pair chosen.
 * <p>
 * <b>Each pair succeeds or fails on its own.</b> Deliberately not transactional
 * here: every publish opens its own transaction, so a class that fails rolls back
 * only itself, and what was released before it stays released. The result says
 * which went and which did not, and a retry sends back only the failures - safe
 * because a publish skips cells that are already current.
 * <p>
 * <b>One email per family</b>, sent once at the end for every class that had
 * something released in a journal that locks. Publishing three journals through
 * the single button would have emailed the same family three times.
 */
@Service
public class BulkPublicationService {

    private static final Logger log = LoggerFactory.getLogger(BulkPublicationService.class);

    @Autowired
    private PublicationService publicationService;

    @Autowired
    private GradebookLookupService lookupService;

    @Autowired
    private GradingTemplateRepository journalRepository;

    @Autowired
    private PeriodRepository periodRepository;

    @Autowired
    private GuardianNotifier guardianNotifier;

    /**
     * @param visible the classes this user may publish for; empty is everything.
     */
    public BulkPublishOptions options(Set<Long> visible) throws SGSException {
        List<ClassGroupOption> classes = lookupService.classes().stream()
                .filter(c -> allows(visible, c.getId()))
                .collect(Collectors.toList());
        List<BulkPublishOptions.Journal> journals = journalRepository.findActive().stream()
                .map(j -> new BulkPublishOptions.Journal(j.getUuid(), j.getName(), j.isLocksOnPublish()))
                .collect(Collectors.toList());
        // A year's classes share one scheme; its periods are the choice for all
        // of them, and periodFor maps a class on another scheme by code.
        List<PeriodOption> periods = classes.isEmpty() ? Collections.emptyList()
                : lookupService.periodsOf(classes.get(0).getId(), null);
        return new BulkPublishOptions(classes, journals, periods);
    }

    /**
     * What each pair would release, writing nothing.
     */
    public List<BulkPublishItem> preview(BulkPublishRequest request, Set<Long> visible)
            throws SGSException {
        requirePeriod(request);
        Names names = names();
        List<BulkPublishItem> items = new ArrayList<>();
        for (BulkPublishRequest.Target target : request.getTargets()) {
            BulkPublishItem item = names.itemFor(target);
            try {
                Long periodId = resolve(target, request.getPeriodId(), visible, names);
                int count = publicationService.pending(
                        target.getClassGroupId(), periodId, target.getJournalUuid());
                item.setCount(count);
                item.setStatus(count > 0 ? BulkPublishItem.Status.READY
                        : BulkPublishItem.Status.UNCHANGED);
            } catch (Exception e) {
                fail(item, e);
            }
            items.add(item);
        }
        return items;
    }

    public List<BulkPublishItem> publish(BulkPublishRequest request, Long actorUserId,
                                         Set<Long> visible) throws SGSException {
        Period period = requirePeriod(request);
        Names names = names();
        List<BulkPublishItem> items = new ArrayList<>();
        Set<Long> toNotify = new LinkedHashSet<>();

        for (BulkPublishRequest.Target target : request.getTargets()) {
            BulkPublishItem item = names.itemFor(target);
            try {
                Long periodId = resolve(target, request.getPeriodId(), visible, names);
                // Nothing new: no release, and no row of zero in the log for it.
                // The single button logs those; across forty classes they would
                // bury the releases that happened.
                if (publicationService.pending(target.getClassGroupId(), periodId,
                        target.getJournalUuid()) == 0) {
                    item.setStatus(BulkPublishItem.Status.UNCHANGED);
                } else {
                    PublicationResult result = publicationService.publish(
                            target.getClassGroupId(), periodId, null, target.getJournalUuid(),
                            actorUserId, false);
                    item.setCount(result.getReleased());
                    item.setStatus(BulkPublishItem.Status.PUBLISHED);
                    if (result.getReleased() > 0
                            && publicationService.journalLocks(target.getJournalUuid())) {
                        toNotify.add(target.getClassGroupId());
                    }
                }
            } catch (Exception e) {
                fail(item, e);
            }
            items.add(item);
        }

        // After every release has committed - each did, in its own transaction.
        if (!toNotify.isEmpty()) {
            List<String> recipients = publicationService.guardianEmailsOf(toNotify);
            if (!recipients.isEmpty()) {
                guardianNotifier.notifyPublished(recipients, null, period.getLabel());
            }
        }
        return items;
    }

    // ---- helpers ------------------------------------------------------------

    private Period requirePeriod(BulkPublishRequest request) throws SGSException {
        if (request.getPeriodId() == null) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "პერიოდი არ არის არჩეული");
        }
        return periodRepository.findById(request.getPeriodId())
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST,
                        "პერიოდი ვერ მოიძებნა"));
    }

    /**
     * Scope first, then the period in this class's scheme. A pair outside the
     * caller's classes is refused here rather than by the console: the console
     * only offers the classes it may, but the request is the caller's to write.
     */
    private Long resolve(BulkPublishRequest.Target target, Long periodId, Set<Long> visible,
                         Names names) throws SGSException {
        if (!allows(visible, target.getClassGroupId())) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "ამ კლასზე წვდომა არ გაქვთ");
        }
        if (!names.journals.containsKey(target.getJournalUuid())) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "ჟურნალი ვერ მოიძებნა");
        }
        return publicationService.periodFor(periodId, target.getClassGroupId());
    }

    private boolean allows(Set<Long> visible, Long classGroupId) {
        return visible.isEmpty() || visible.contains(classGroupId);
    }

    /**
     * A failed pair carries the reason in the school's words where there is one.
     * Anything else is a fault worth a log line, and the director is told only
     * that it failed.
     */
    private void fail(BulkPublishItem item, Exception e) {
        item.setStatus(BulkPublishItem.Status.FAILED);
        if (e instanceof SGSException && e.getMessage() != null) {
            item.setError(e.getMessage());
        } else {
            log.warn("bulk publish failed for class {} journal {}",
                    item.getClassGroupId(), item.getJournalUuid(), e);
            item.setError("ვერ გამოქვეყნდა - სცადეთ თავიდან");
        }
    }

    private Names names() {
        Names names = new Names();
        names.classes = lookupService.classes().stream()
                .collect(Collectors.toMap(ClassGroupOption::getId, ClassGroupOption::getName,
                        (a, b) -> a));
        names.journals = journalRepository.findActive().stream()
                .collect(Collectors.toMap(GradingTemplate::getUuid, Function.identity(),
                        (a, b) -> a));
        return names;
    }

    /**
     * Class and journal names, read once per request rather than once per pair.
     */
    private static final class Names {
        private Map<Long, String> classes;
        private Map<String, GradingTemplate> journals;

        private BulkPublishItem itemFor(BulkPublishRequest.Target target) {
            BulkPublishItem item = new BulkPublishItem();
            item.setClassGroupId(target.getClassGroupId());
            item.setClassName(classes.get(target.getClassGroupId()));
            item.setJournalUuid(target.getJournalUuid());
            GradingTemplate journal = journals.get(target.getJournalUuid());
            item.setJournalName(journal == null ? null : journal.getName());
            return item;
        }
    }
}
