package com.claimsys.service;

import com.claimsys.model.Claim;
import com.claimsys.model.Priority;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.time.LocalDate;

/**
 * Rule-based claim priority scoring (0-100).
 * This is the extension point for the optional Python ML model: replace score()
 * with a REST call to a Flask/FastAPI service that returns {"risk": 82}.
 */
@Service
public class RiskScoringService {

    public record Result(int score, Priority priority) {}

    public Result score(Claim c) {
        int score = 10;
        BigDecimal amt = c.getAmount();
        if (amt.compareTo(new BigDecimal("500000")) >= 0) score += 45;
        else if (amt.compareTo(new BigDecimal("100000")) >= 0) score += 30;
        else if (amt.compareTo(new BigDecimal("25000")) >= 0) score += 15;

        String type = c.getType() == null ? "" : c.getType().toLowerCase();
        if (type.contains("accident") || type.contains("injury") || type.contains("fire") || type.contains("theft")) score += 20;
        else if (type.contains("damage")) score += 10;

        long daysSinceIncident = ChronoUnit.DAYS.between(c.getIncidentDate(), LocalDate.now());
        if (daysSinceIncident > 30) score += 15; // late reporting is a fraud signal

        score = Math.min(score, 100);
        Priority p = score >= 70 ? Priority.HIGH : score >= 40 ? Priority.MEDIUM : Priority.LOW;
        return new Result(score, p);
    }
}
