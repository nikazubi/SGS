package mthiebi.sgs.gradebook;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The layer the rest of the suite does not reach.
 * <p>
 * Decision 21 said tests would be narrow but real - the engine, not controllers
 * or wiring. That was defensible until a review found four faults that each
 * stopped the product working, and every one of them sat in this gap: a login
 * endpoint left out of the permitAll list, a security filter that could not
 * resolve a token, a checked exception that quietly committed, and a request
 * body serialised into a query string.
 * <p>
 * None of those are logic. They are wiring, and wiring is only testable by
 * starting the thing. This boots the real application context and speaks HTTP
 * to it; it does not assert on data, which the integration tests already cover.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApplicationWiringIT {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("the application context starts")
    void contextLoads() {
        // Catches a missing @EnableAsync, an unsatisfiable bean, a bad
        // @Scheduled signature - anything that leaves the app dead on arrival
        // while every unit test still passes.
    }

    @Test
    @DisplayName("parent login is reachable without a token")
    void parentLoginIsPubliclyReachable() throws Exception {
        // It was not: /api/parent/authenticate was missing from the permitAll
        // list, so it answered 401 and a parent needed a token to get a token.
        // Credentials are deliberately wrong - anything but 401/403 proves the
        // endpoint is reachable, which is the point.
        mvc.perform(post("/api/parent/authenticate")
                        .contentType("application/json")
                        .content("{\"username\":\"nobody\",\"password\":\"nothing\"}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 401 || status == 403) {
                        throw new AssertionError(
                                "parent login must not require a token, got " + status);
                    }
                });
    }

    @Test
    @DisplayName("everything else under the parent portal does require one")
    void parentDataRequiresAToken() throws Exception {
        // The counterpart, and the reason the previous test is not simply
        // "permit /api/parent/**": the legacy /client/** is open in its
        // entirety, and a portal serving a child's grades must not be.
        mvc.perform(get("/api/parent/journals"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        throw new AssertionError("parent data must not be readable anonymously");
                    }
                });
    }

    @Test
    @DisplayName("staff endpoints are not anonymous either")
    void staffEndpointsRequireAToken() throws Exception {
        mvc.perform(get("/api/gradebook/journals"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        throw new AssertionError("journals must not be listable anonymously");
                    }
                });
    }

    @Test
    @DisplayName("a malformed parent token is refused, not served")
    void malformedParentTokenIsRefused() throws Exception {
        // The token subject is a student id. A token carrying a username - which
        // is what the legacy login issues - must not resolve to whichever
        // student happens to hold that id.
        mvc.perform(get("/api/parent/journals")
                        .header("authorization", "Bearer not.a.real.token"))
                .andExpect(status().is4xxClientError());
    }
}
