package mthiebi.sgs.SMTP;

public interface EmailService {

    /**
     * Sends, and reports the outcome as a string.
     * <p>
     * Swallows every failure. Kept because existing callers treat mail as
     * fire-and-forget, and a publication must not fail because a mail server is
     * down.
     */
    String sendSimpleMail(EmailDetails details);

    /**
     * Sends, and throws if it did not go.
     * <p>
     * For callers that record what they have sent and must retry what they have
     * not. The string-returning version above cannot be used for that: it
     * reports failure in a value nobody checks, so an absence notice was being
     * marked as delivered while the mail was lost.
     */
    void sendOrThrow(EmailDetails details);
}
