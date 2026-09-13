package kz.iitu.spring_lab_01.notify;

import jakarta.annotation.PostConstruct;
import org.slf4j.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("masking")
@Order(3)
public class MaskingNotifier implements Notifier {
    private static final Logger log = LoggerFactory.getLogger(MaskingNotifier.class);

    @PostConstruct
    void init() {
        log.info("MaskingNotifier initialized");
    }

    @Override
    public String send(String message) {
        return message.replaceAll("\\d", "*");
    }

    @Override
    public String channel() { return "masking"; }
}