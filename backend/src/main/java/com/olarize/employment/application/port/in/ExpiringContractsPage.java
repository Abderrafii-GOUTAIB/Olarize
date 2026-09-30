package com.olarize.employment.application.port.in;

import java.util.List;

public record ExpiringContractsPage(List<ExpiringContract> items, long totalItems) {
    public ExpiringContractsPage { items = List.copyOf(items); }
}
