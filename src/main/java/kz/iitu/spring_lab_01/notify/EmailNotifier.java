package kz.iitu.spring_lab_01.notify;

import org.slf4j.*;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("email")
@Primary // chosen by default
@Order(2)
class EmailNotifier implements Notifier {
    private static final Logger log = LoggerFactory.getLogger(EmailNotifier.class);

    @Override
    public String send(String message) {
        log.info("EMAIL >> {}", message);
        return "email: " + message;
    }

    @Override
    public String channel() { return "email"; }
}