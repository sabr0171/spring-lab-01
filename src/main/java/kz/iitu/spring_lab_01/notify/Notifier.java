package kz.iitu.spring_lab_01.notify;

public interface Notifier {

    String send(String message);   // returns what was "sent"

    String channel();              // channel name for the report
}