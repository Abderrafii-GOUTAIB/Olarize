package com.olarize.employment.application.port.out;

import com.olarize.employment.application.port.in.ExpiringContract;
import com.olarize.employment.application.port.in.ExpiringContractsPage;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ExpiringContractsPort {
    ExpiringContractsPage findCurrentVersions(UUID companyId, LocalDate fromDate,
            LocalDate toDate, int page, int pageSize);
    long countCurrentContractsWithoutEndDate(UUID companyId);
}
