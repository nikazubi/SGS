package mthiebi.sgs.gradebook.service.absence;

import mthiebi.sgs.gradebook.model.AbsenceNotice;
import mthiebi.sgs.gradebook.model.Enrollment;
import mthiebi.sgs.gradebook.repository.AbsenceNoticeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Telling a parent their child was absent - a quarter of an hour later.
 * <p>
 * Marking a cell autosaves in about a second. Sending on that would mean a
 * mis-click tells a parent their child was absent, with no unsend. So marking
 * queues a notice and this sends it later.
 * <p>
 * **The cell is re-read at send time.** That is what makes the delay a
 * correction window rather than a race: a mark withdrawn at any point before
 * the job runs results in no message, and nobody has to beat a timer.
 * <p>
 * One pending notice per student per day, so a child absent from several lessons
 * produces one message.
 */
@Service
public class AbsenceNotifier {

    private static final Logger log = LoggerFactory.getLogger(AbsenceNotifier.class);

    @Autowired
    private AbsenceNoticeRepository absenceNoticeRepository;

    @Autowired
    private AbsenceNoticeSender sender;

    /**
     * How long a mark sits before anyone is told.
     * <p>
     * The school asked for "a bit later, but not hours" - a parent should know
     * during the school day. Configurable because the right number is a matter
     * of how the office actually works, not of design.
     */
    @Value("${sgs.absence.notify-after-minutes:15}")
    private int windowMinutes;

    @Value("${sgs.absence.notify-enabled:true}")
    private boolean enabled;

    /**
     * Queued when a cell is marked absent.
     * <p>
     * Idempotent per student per day *while a notice is still pending*: marking
     * a second lesson absent finds the existing one. A notice already resolved
     * is not reused - a mark withdrawn in the morning and a genuine absence in
     * the afternoon are two different things to tell a parent about, and reusing
     * the dead row meant the second one was silently never sent.
     * <p>
     * Its own transaction, because the caller has none. A duplicate lost to a
     * race throws out of here and is caught by the caller, which keeps the mark:
     * failing to queue an email must never cost the register a row.
     */
    @Transactional(rollbackFor = Exception.class)
    public void queue(Enrollment enrollment, LocalDate date) {
        if (!absenceNoticeRepository.findPending(enrollment.getId(), date).isEmpty()) {
            return;
        }
        AbsenceNotice notice = new AbsenceNotice();
        notice.setEnrollment(enrollment);
        notice.setAbsenceDate(date);
        notice.setQueuedAt(Instant.now());
        // saveAndFlush, not save. The id comes from a sequence, so save() only
        // queues the insert and any violation of db/025's filtered unique index
        // would surface at commit - outside every catch, and after the caller
        // believed it had succeeded. The caller treats a failure here as
        // non-fatal: the mark is the record, the email is a courtesy on top.
        absenceNoticeRepository.saveAndFlush(notice);
    }

    /**
     * Sends what is due, having checked it is still true.
     * <p>
     * Deliberately **not** transactional. Each notice is sent in its own
     * transaction by {@link AbsenceNoticeSender}: one shared transaction around
     * a batch of sends means a failure part way through rolls back the rows
     * marking the already-delivered messages as sent, and the next run delivers
     * them all a second time.
     * <p>
     * A notice that fails to send stays pending, so the next run retries it. A
     * mail server down for an hour delays messages; it does not lose them.
     */
    @Scheduled(fixedDelayString = "${sgs.absence.poll-seconds:120}000")
    public void sendDue() {
        if (!enabled) {
            return;
        }
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(windowMinutes));
        List<Long> due = pendingIds(cutoff);

        int failed = 0;
        for (Long id : due) {
            try {
                sender.sendOne(id);
            } catch (Exception e) {
                failed++;
                log.warn("absence notice {} not sent, will retry: {}", id, e.getMessage());
            }
        }
        if (failed > 0) {
            log.warn("{} of {} absence notices deferred", failed, due.size());
        }
    }

    /**
     * The ids due, settled before any sending starts.
     * <p>
     * The annotation here does nothing when sendDue calls it - a self-call does
     * not pass through Spring's proxy, which is the same trap AbsenceNoticeSender
     * exists to avoid. It is kept because the repository call opens its own
     * transaction regardless, and because the method is worth calling directly
     * from a test. The comment is here so nobody reads the annotation as a
     * guarantee it is not making.
     */
    @Transactional(readOnly = true)
    public List<Long> pendingIds(Instant cutoff) {
        return absenceNoticeRepository.findDue(cutoff).stream()
                .map(AbsenceNotice::getId)
                .collect(Collectors.toList());
    }
}
