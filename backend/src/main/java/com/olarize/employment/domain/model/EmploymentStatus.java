package com.olarize.employment.domain.model;

import java.time.LocalDate;

public enum EmploymentStatus {
    UPCOMING, CURRENT, ENDED;

    public static EmploymentStatus asOf(LocalDate startDate, LocalDate lastEmployedDay, LocalDate asOf) {
        if (startDate == null || asOf == null || (lastEmployedDay != null && lastEmployedDay.isBefore(startDate))) {
            throw new IllegalArgumentException("Invalid employment dates");
        }
        if (startDate.isAfter(asOf)) return UPCOMING;
        return lastEmployedDay != null && lastEmployedDay.isBefore(asOf) ? ENDED : CURRENT;
    }
}
