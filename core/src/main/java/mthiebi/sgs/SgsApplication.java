package mthiebi.sgs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "mthiebi.sgs.models")
@EnableJpaRepositories(basePackages = "mthiebi.sgs.repository")
public class SgsApplication extends SpringBootServletInitializer {
    public static void main(String[] args) {
        SpringApplication.run(SgsApplication.class, args);
    }
}
