package com.olarize.employment.application.port.in;

import java.time.LocalDate;
import java.util.UUID;

public interface ListExpiringContractsUseCase {
    ExpiringContractsResult list(UUID companyId, LocalDate fromDate, LocalDate toDate,
            int page, int pageSize);
}
