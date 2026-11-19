package kz.iitu.spring_lab_01.fine;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// Lab 12, variant 6: how much a day costs and where the fine stops (app.fines.* in application.properties).
// A plain record, so a unit test simply writes new FinePolicy(100, 2000).
@Validated
@ConfigurationProperties(prefix = "app.fines")
public record FinePolicy(@Positive int dailyRate, @Positive int maximum) {
}
