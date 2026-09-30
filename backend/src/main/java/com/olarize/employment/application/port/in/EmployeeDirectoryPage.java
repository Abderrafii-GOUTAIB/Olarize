package com.olarize.employment.application.port.in;

import java.util.List;

public record EmployeeDirectoryPage(List<EmployeeRecord> items, long totalItems) {
    public EmployeeDirectoryPage { items = List.copyOf(items); }
}
