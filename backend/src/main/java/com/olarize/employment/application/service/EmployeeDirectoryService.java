package com.olarize.employment.application.service;

import com.olarize.employment.application.exception.EmployeeNotFound;
import com.olarize.employment.application.port.in.EmployeeDetailResult;
import com.olarize.employment.application.port.in.EmployeeDirectoryResult;
import com.olarize.employment.application.port.in.EmployeeDirectoryUseCase;
import com.olarize.employment.application.port.out.EmployeeDirectoryPort;
import com.olarize.employment.domain.model.EmployeeDirectoryFilter;
import com.olarize.identity.application.port.in.CompanyAccessUseCase;
import com.olarize.platform.api.DatabaseScopeSigner;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class EmployeeDirectoryService implements EmployeeDirectoryUseCase {
    private static final ZoneId ZONE = ZoneId.of("Africa/Casablanca");
    private final CompanyAccessUseCase access;
    private final DatabaseScopeSigner scope;
    private final JdbcTemplate database;
    private final EmployeeDirectoryPort records;
    private final TransactionTemplate transaction;
    private final Clock clock;

    @Autowired
    public EmployeeDirectoryService(CompanyAccessUseCase access, DatabaseScopeSigner scope,
            JdbcTemplate database, EmployeeDirectoryPort records, PlatformTransactionManager transactions) {
        this(access, scope, database, records, transactions, Clock.system(ZONE));
    }

    EmployeeDirectoryService(CompanyAccessUseCase access, DatabaseScopeSigner scope,
            JdbcTemplate database, EmployeeDirectoryPort records, PlatformTransactionManager transactions, Clock clock) {
        this.access = access;
        this.scope = scope;
        this.database = database;
        this.records = records;
        this.transaction = new TransactionTemplate(transactions);
        this.transaction.setReadOnly(true);
        this.clock = clock.withZone(ZONE);
    }

    @Override
    public EmployeeDirectoryResult list(UUID companyId, LocalDate asOf, EmployeeDirectoryFilter filter) {
        if (filter == null) throw new IllegalArgumentException("Directory filter is required");
        LocalDate date = asOf == null ? LocalDate.now(clock) : asOf;
        return read(companyId, () -> {
            var page = records.find(companyId, date, filter);
            int pages = Math.toIntExact((page.totalItems() + filter.pageSize() - 1) / filter.pageSize());
            return new EmployeeDirectoryResult(companyId, date, ZONE.getId(), page,
                    filter.page(), filter.pageSize(), pages);
        });
    }

    @Override
    public EmployeeDetailResult detail(UUID companyId, UUID employeeId, LocalDate asOf) {
        if (employeeId == null) throw new IllegalArgumentException("Employee is required");
        LocalDate date = asOf == null ? LocalDate.now(clock) : asOf;
        return read(companyId, () -> new EmployeeDetailResult(records.findById(companyId, employeeId, date)
                .orElseThrow(EmployeeNotFound::new), date, ZONE.getId()));
    }

    private <T> T read(UUID companyId, Supplier<T> operation) {
        if (companyId == null) throw new IllegalArgumentException("Company is required");
        access.require(companyId, "employee.read");
        String actor = access.currentUserId();
        return transaction.execute(status -> {
            scope.apply(database, actor, companyId);
            return operation.get();
        });
    }
}
