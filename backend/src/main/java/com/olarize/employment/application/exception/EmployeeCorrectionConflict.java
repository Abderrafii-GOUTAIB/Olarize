package com.olarize.employment.application.exception;

import java.util.List;

public class EmployeeCorrectionConflict extends RuntimeException {
    private final List<String> dependentFacts;
    public EmployeeCorrectionConflict() { this(List.of()); }
    public EmployeeCorrectionConflict(List<String> dependentFacts) {
        this.dependentFacts = List.copyOf(dependentFacts);
    }
    public List<String> dependentFacts() { return dependentFacts; }
}
