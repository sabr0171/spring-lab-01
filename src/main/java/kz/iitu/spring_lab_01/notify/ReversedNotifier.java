package kz.iitu.spring_lab_01.notify;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("reversed")
@Order(3)
public class ReversedNotifier implements kz.iitu.spring_lab_01.notify.Notifier {

    private static final Logger log = LoggerFactory.getLogger(ReversedNotifier.class);

    @PostConstruct
    void init() {
        log.info("ReversedNotifier initialized");
    }

    @Override
    public String send(String message) {
        return new StringBuilder(message).reverse().toString();
    }

    @Override
    public String channel() {
        return "reversed";
    }
}