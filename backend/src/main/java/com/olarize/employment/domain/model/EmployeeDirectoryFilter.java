package com.olarize.employment.domain.model;

import java.util.Locale;

public record EmployeeDirectoryFilter(String search, EmployeeDirectoryStatus status, int page, int pageSize) {
    public EmployeeDirectoryFilter {
        if (status == null || page < 0 || pageSize < 1 || pageSize > 100
                || (long) page * pageSize > Integer.MAX_VALUE
                || (search != null && search.length() > 200)) {
            throw new IllegalArgumentException("Invalid directory filter");
        }
        search = search == null || search.isBlank() ? null : search.strip();
    }

    public String searchPattern() {
        if (search == null) return "%";
        return "%" + search.toLowerCase(Locale.ROOT).replace("!", "!!")
                .replace("%", "!%").replace("_", "!_") + "%";
    }
}
