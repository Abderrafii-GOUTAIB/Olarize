package com.olarize.employment.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

/** Full minimum work-fact replacement, with an explicit job effective date. */
public record EmployeeCorrection(EmployeeCreation facts, LocalDate effectiveDate, String reason) {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9][0-9 ()-]{0,31}$");

    public EmployeeCorrection {
        if (facts == null || effectiveDate == null || reason == null || reason.isBlank()
                || reason.trim().length() > 1000 || effectiveDate.isBefore(facts.startDate())
                || (facts.workEmail() != null && !EMAIL.matcher(facts.workEmail()).matches())
                || (facts.workPhone() != null && !PHONE.matcher(facts.workPhone()).matches())) {
            throw new IllegalArgumentException("Invalid correction facts");
        }
        facts = EmployeeCreation.of(facts.reference(), facts.name(), facts.startDate(), facts.jobTitle(),
                facts.department(), facts.site(), facts.workEmail(), facts.workPhone());
        reason = reason.trim();
    }

    public static List<String> startConflicts(LocalDate start, LocalDate lastDay,
            List<LocalDate> jobDates, boolean overlappingEpisode) {
        var conflicts = new ArrayList<String>();
        if (lastDay != null && start.isAfter(lastDay)) conflicts.add("LAST_EMPLOYED_DAY");
        if (jobDates.stream().anyMatch(date -> date.isBefore(start))) conflicts.add("JOB_INTERVAL");
        if (overlappingEpisode) conflicts.add("OTHER_EPISODE");
        return List.copyOf(conflicts);
    }
}
