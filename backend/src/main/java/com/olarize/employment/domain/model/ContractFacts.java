package com.olarize.employment.domain.model;

import java.time.LocalDate;

/** Facts supplied by HR; this model does not assess legal validity. */
public record ContractFacts(ContractType typeCode, LocalDate startDate, LocalDate endDate,
        String sourceReference) {
    public static ContractFacts of(ContractType typeCode, LocalDate startDate,
            LocalDate endDate, String sourceReference) {
        if (typeCode == null || startDate == null) throw new IllegalArgumentException("Required contract fact is missing");
        if (endDate != null && endDate.isBefore(startDate)) throw new IllegalArgumentException("Contract end precedes start");
        if (sourceReference == null || sourceReference.isBlank() || sourceReference.trim().length() > 200) {
            throw new IllegalArgumentException("A bounded source reference is required");
        }
        return new ContractFacts(typeCode, startDate, endDate, sourceReference.trim());
    }
}
