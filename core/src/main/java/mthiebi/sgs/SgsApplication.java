package mthiebi.sgs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// GuardianNotifier is @Async: without this it ran synchronously on the request
// thread, so a slow mail server made publishing slow after all.
@org.springframework.scheduling.annotation.EnableAsync
@org.springframework.scheduling.annotation.EnableScheduling
@SpringBootApplication
@EntityScan(basePackages = {"mthiebi.sgs.models", "mthiebi.sgs.gradebook.model"})
@EnableJpaRepositories(basePackages = {"mthiebi.sgs.repository", "mthiebi.sgs.gradebook.repository"})
public class SgsApplication extends SpringBootServletInitializer {
    public static void main(String[] args) {
        SpringApplication.run(SgsApplication.class, args);
    }
}
