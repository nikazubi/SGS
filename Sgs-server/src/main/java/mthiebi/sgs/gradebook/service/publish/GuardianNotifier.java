package mthiebi.sgs.gradebook.service.publish;

import mthiebi.sgs.SMTP.EmailDetails;
import mthiebi.sgs.SMTP.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tells guardians that something has changed.
 * <p>
 * Off the request thread and outside the transaction. The legacy publish sent
 * one message per student inline, inside the loop that wrote the close events -
 * about 900 synchronous sends for a full release - so a slow mail server made
 * publishing slow and a failing one could fail it outright.
 * <p>
 * A send that fails is logged and dropped. It is a notification: parents can
 * still see the grades, and losing the email is far better than losing the
 * publication.
 */
@Component
public class GuardianNotifier {

    private static final Logger log = LoggerFactory.getLogger(GuardianNotifier.class);

    private static final String PORTAL = "https://www.ibmthiebistudentrating.edu.ge";

    @Autowired
    private EmailService emailService;

    @Async
    public void notifyPublished(List<String> recipients, String className, String periodLabel) {
        String body = "გაცნობებთ, რომ " + PORTAL + " პორტალზე ატვირთულია "
                + periodLabel + "-ის ნიშნები.";
        send(recipients, "IB მთიები - ნიშნების განახლება", body);
    }

    @Async
    public void notifyChangeApproved(String recipient, String comment) {
        if (recipient == null || recipient.length() <= 5) {
            return;
        }
        send(java.util.Collections.singletonList(recipient),
                "IB მთიები - ნიშნის ცვლილება",
                comment == null || comment.isEmpty()
                        ? "გაცნობებთ, რომ თქვენი შვილის ნიშანი შეიცვალა."
                        : comment);
    }

    private void send(List<String> recipients, String subject, String body) {
        int failed = 0;
        for (String recipient : recipients) {
            try {
                emailService.sendSimpleMail(EmailDetails.builder()
                        .recipient(recipient)
                        .subject(subject)
                        .msgBody(body)
                        .build());
            } catch (Exception e) {
                failed++;
                log.warn("could not notify {}: {}", recipient, e.getMessage());
            }
        }
        if (failed > 0) {
            log.warn("{} of {} notifications failed", failed, recipients.size());
        }
    }
}
