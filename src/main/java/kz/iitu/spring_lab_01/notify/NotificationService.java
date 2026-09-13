package kz.iitu.spring_lab_01.notify;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class NotificationService {
    private final Notifier primary;
    private final Notifier console;
    private final Notifier masking;
    private final List<Notifier> all;
    private final Map<String, Notifier> byName;

    public NotificationService(Notifier primary,
                               @Qualifier("console") Notifier console,
                               @Qualifier("masking") Notifier masking,
                               List<Notifier> all,
                               Map<String, Notifier> byName) {
        this.primary = primary;
        this.console = console;
        this.masking = masking;
        this.all = all;
        this.byName = byName;
    }

    public String viaPrimary(String message) { return primary.send(message); }
    public String viaConsole(String message) { return console.send(message); }
    public String viaMasking(String message) { return masking.send(message); }
    public List<String> viaAll(String message) {
        return all.stream().map(n -> n.send(message)).toList();
    }
    public Set<String> names() { return byName.keySet(); }
}
