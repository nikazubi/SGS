package mthiebi.sgs.gradebook.publish;

import mthiebi.sgs.SMTP.EmailDetails;
import mthiebi.sgs.SMTP.EmailService;
import mthiebi.sgs.gradebook.service.publish.GuardianNotifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * The grade emails: a failed send is noticed and survived.
 * <p>
 * The notifier used to call sendSimpleMail, which catches every failure itself
 * and hands it back as a string nobody read - so the notifier's own catch, and
 * the warning in it, never ran. Here the mail service throws the way
 * sendOrThrow does, and the rest of the list still goes out.
 */
@ExtendWith(MockitoExtension.class)
class GuardianNotifierTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private GuardianNotifier notifier;

    @Test
    @DisplayName("one address failing does not stop the others, and nothing is thrown")
    void oneFailureDoesNotStopTheRest() {
        doThrow(new RuntimeException("connection refused"))
                .when(emailService).sendOrThrow(argThat(d -> "b@example.ge".equals(d.getRecipient())));

        assertDoesNotThrow(() -> notifier.notifyPublished(
                Arrays.asList("a@example.ge", "b@example.ge", "c@example.ge"), null, "I ტრიმესტრი"));

        ArgumentCaptor<EmailDetails> sent = ArgumentCaptor.forClass(EmailDetails.class);
        verify(emailService, times(3)).sendOrThrow(sent.capture());
        assertEquals(Arrays.asList("a@example.ge", "b@example.ge", "c@example.ge"),
                Arrays.asList(sent.getAllValues().stream().map(EmailDetails::getRecipient).toArray()));
        // The swallowing method is not used at all, so a failure cannot vanish into it.
        verify(emailService, never()).sendSimpleMail(any());
    }
}
