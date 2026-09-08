package kz.iitu.spring_lab_01.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Arrays;
import java.util.List;

// REST-контроллер с эндпоинтами /api/hello и /api/info
@RestController
@RequestMapping("/api")
public class HelloController {

    @Value("${app.owner:unknown}")
    private String owner;

    @GetMapping("/hello")
    public Greeting hello(@RequestParam(defaultValue = "world") String name) {
        return new Greeting("Hello, " + name + "!", owner, LocalDateTime.now());
    }

    @GetMapping("/info")
    public Info info() {
        return new Info(owner,
                System.getProperty("java.version"),
                Runtime.getRuntime().availableProcessors());
    }

    @GetMapping("/stats")
    public Stats stats(@RequestParam(required = false) String numbers) {
        if (numbers == null || numbers.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Parameter 'numbers' is required, e.g. /api/stats?numbers=1,2,3");
        }

        List<Double> values;
        try {
            values = Arrays.stream(numbers.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Double::parseDouble)
                    .toList();
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid number format in 'numbers'");
        }

        if (values.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Parameter 'numbers' must contain at least one number");
        }

        double min = values.stream().min(Double::compareTo).get();
        double max = values.stream().max(Double::compareTo).get();
        double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        return new Stats(values, min, max, avg);
    }

    public record Greeting(String message, String owner, LocalDateTime timestamp) { }
    public record Info(String owner, String javaVersion, int cpuCores) { }
    public record Stats(List<Double> numbers, double min, double max, double average) { }
}