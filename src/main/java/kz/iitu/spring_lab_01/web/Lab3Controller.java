package kz.iitu.spring_lab_01.web;

import kz.iitu.spring_lab_01.config.*;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static java.util.Map.entry;

@RestController
@RequestMapping("/api/lab3")
public class Lab3Controller {

    private final AppProperties props;
    private final EnvironmentBanner banner;
    private final Environment environment;

    public Lab3Controller(AppProperties props, EnvironmentBanner banner,
                          Environment environment) {
        this.props = props;
        this.banner = banner;
        this.environment = environment;
    }

    @GetMapping("/config")
    public Map<String, Object> config() {
        return Map.ofEntries(
                entry("owner", props.owner()),
                entry("group", props.group()),
                entry("mailFrom", props.mail().from()),
                entry("mailRetryCount", props.mail().retryCount()),
                entry("mailTimeout", props.mail().timeout().toString()),
                entry("mailEnabled", props.mail().enabled()),
                entry("requestsPerMinute", props.rateLimit().requestsPerMinute()),
                entry("burst", props.rateLimit().burst()),
                entry("serverPort", environment.getProperty("server.port")),
                entry("activeProfiles", Arrays.asList(environment.getActiveProfiles())),
                entry("banner", banner.describe()));
    }
}
