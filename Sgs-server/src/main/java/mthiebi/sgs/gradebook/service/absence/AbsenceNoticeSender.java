package mthiebi.sgs.gradebook.service.absence;

import mthiebi.sgs.SMTP.EmailDetails;
import mthiebi.sgs.SMTP.EmailService;
import mthiebi.sgs.gradebook.model.AbsenceNotice;
import mthiebi.sgs.gradebook.repository.AbsenceNoticeRepository;
import mthiebi.sgs.gradebook.repository.DailyAbsenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Sending one absence notice, in its own transaction.
 * <p>
 * A separate bean rather than a method on {@link AbsenceNotifier}, and not for
 * tidiness: Spring's transaction proxy is bypassed by a self-call, so an
 * annotation on a private neighbour would silently do nothing and the whole
 * batch would keep sharing one transaction. That matters because the sends are
 * inside it - a failure part way through a batch would roll back the rows
 * marking the *already delivered* messages as sent, and the next run would
 * deliver them all again.
 * <p>
 * One notice per transaction: a message that goes is recorded as gone, whatever
 * happens to the next one.
 * <p>
 * REQUIRED rather than REQUIRES_NEW, deliberately. {@code sendDue} is not
 * transactional, so REQUIRED already opens a fresh transaction per notice - the
 * separation that matters is the *bean*, not the propagation. REQUIRES_NEW adds
 * nothing here and costs a great deal: a caller that already holds a transaction
 * on absence_notice - every @DataJpaTest, whose rows are never committed - is
 * invisible to the new transaction, which then blocks on its locks until the
 * suite times out. It hung the build rather than failing it.
 */
@Service
public class AbsenceNoticeSender {

    @Autowired
    private AbsenceNoticeRepository absenceNoticeRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private DailyAbsenceRepository dailyAbsenceRepository;

    /**
     * @return true when the notice was resolved - sent or deliberately not sent.
     * False means it stays pending and the next run will try again.
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public boolean sendOne(Long noticeId) {
        AbsenceNotice notice = absenceNoticeRepository.findById(noticeId).orElse(null);
        if (notice == null || notice.getSentAt() != null) {
            return true;
        }

        if (!stillAbsent(notice)) {
            // Withdrawn inside the window. Recorded rather than deleted, so
            // "we decided not to tell them" stays visible.
            notice.setCancelled(true);
            notice.setSentAt(Instant.now());
            absenceNoticeRepository.save(notice);
            return true;
        }

        String recipient = notice.getEnrollment().getStudent().getGuardianEmail();
        if (recipient == null || recipient.trim().length() <= 5) {
            // No usable address. Cancelled rather than marked sent: "nobody was
            // told" and "somebody was told" must not look the same afterwards.
            notice.setCancelled(true);
            notice.setSentAt(Instant.now());
            absenceNoticeRepository.save(notice);
            return true;
        }

        // sendOrThrow, not sendSimpleMail: the latter reports failure in a
        // returned string that nobody checks, so a notice was being stamped as
        // delivered while the mail was lost.
        emailService.sendOrThrow(EmailDetails.builder()
                .recipient(recipient.trim())
                .subject("IB მთიები - გაცდენა")
                .msgBody("გაცნობებთ, რომ " + notice.getAbsenceDate() + " თარიღით "
                        + notice.getEnrollment().getStudent().getFirstName()
                        + " გაკვეთილს არ დაესწრო.")
                .build());

        notice.setSentAt(Instant.now());
        absenceNoticeRepository.save(notice);
        return true;
    }

    /**
     * Is the child still marked absent on that day?
     * <p>
     * Read now rather than trusted from when it was queued - the whole point of
     * the delay.
     * <p>
     * A row exists or it does not. This was a query over grade_entry that loaded
     * every cell on the day period and asked whether any of them held a number
     * greater than zero, because "absent" was the value 1 and "present" was the
     * absence of a value - the ambiguity that cost the register its correctness.
     */
    private boolean stillAbsent(AbsenceNotice notice) {
        return !dailyAbsenceRepository.findCell(
                notice.getEnrollment().getId(), notice.getAbsenceDate()).isEmpty();
    }
}
