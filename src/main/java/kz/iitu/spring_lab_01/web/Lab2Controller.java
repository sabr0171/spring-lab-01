package kz.iitu.spring_lab_01.web;

import kz.iitu.spring_lab_01.notify.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/lab2")
public class Lab2Controller {

    private final NotificationService notifications;

    public Lab2Controller(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping("/notify")
    public Map<String, Object> notify(@RequestParam(name = "text", defaultValue = "Hello") String text) {
        return Map.of("primary",   notifications.viaPrimary(text),
                "console",   notifications.viaConsole(text),
                "all",       notifications.viaAll(text),
                "beanNames", notifications.names());
    }

    @GetMapping("/custom")
    public String custom(@RequestParam(name = "text", defaultValue = "Hello") String text) {
        return notifications.viaReversed(text);
    }
}