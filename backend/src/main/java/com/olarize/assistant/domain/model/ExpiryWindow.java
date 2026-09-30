package com.olarize.assistant.domain.model;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeParseException;
import java.util.Set;

public record ExpiryWindow(LocalDate fromDate, LocalDate toDate) {
    public ExpiryWindow {
        if (fromDate == null || toDate == null || toDate.isBefore(fromDate)
                || ChronoUnit.DAYS.between(fromDate, toDate) >= 366) throw new IllegalArgumentException("Invalid expiry window");
    }
    public static ExpiryWindow resolve(ExpiryToolCall call, Clock clock) {
        if (!"listExpiringContracts".equals(call.toolName())) throw new IllegalArgumentException("Unsupported tool");
        var arguments = call.arguments();
        if (arguments.keySet().equals(Set.of("period")) && "NEXT_MONTH".equals(arguments.get("period"))) {
            var next = YearMonth.from(LocalDate.now(clock.withZone(ZoneId.of("Africa/Casablanca")))).plusMonths(1);
            return new ExpiryWindow(next.atDay(1), next.atEndOfMonth());
        }
        if (!arguments.keySet().equals(Set.of("fromDate", "toDate"))) throw new IllegalArgumentException("Unsupported arguments");
        try { return new ExpiryWindow(LocalDate.parse(arguments.get("fromDate")), LocalDate.parse(arguments.get("toDate"))); }
        catch (DateTimeParseException invalid) { throw new IllegalArgumentException("Invalid expiry dates"); }
    }
}
