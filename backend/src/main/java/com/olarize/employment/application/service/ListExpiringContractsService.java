package com.olarize.employment.application.service;

import com.olarize.employment.application.port.in.ExpiringContractsResult;
import com.olarize.employment.application.port.in.ListExpiringContractsUseCase;
import com.olarize.employment.application.port.out.ExpiringContractsPort;
import com.olarize.employment.domain.model.ContractExpiryWindow;
import com.olarize.identity.application.port.in.CompanyAccessUseCase;
import com.olarize.platform.api.DatabaseScopeSigner;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ListExpiringContractsService implements ListExpiringContractsUseCase {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Africa/Casablanca");
    private static final int MAX_PAGE_SIZE = 100;
    private final CompanyAccessUseCase access;
    private final DatabaseScopeSigner scope;
    private final JdbcTemplate database;
    private final ExpiringContractsPort contracts;
    private final TransactionTemplate transaction;
    private final Clock clock;

    @Autowired
    public ListExpiringContractsService(CompanyAccessUseCase access, DatabaseScopeSigner scope,
            JdbcTemplate database, ExpiringContractsPort contracts,
            PlatformTransactionManager transactions) {
        this(access, scope, database, contracts, transactions, Clock.system(BUSINESS_ZONE));
    }

    ListExpiringContractsService(CompanyAccessUseCase access, DatabaseScopeSigner scope,
            JdbcTemplate database, ExpiringContractsPort contracts,
            PlatformTransactionManager transactions, Clock clock) {
        this.access = access;
        this.scope = scope;
        this.database = database;
        this.contracts = contracts;
        this.transaction = new TransactionTemplate(transactions);
        this.transaction.setReadOnly(true);
        this.clock = clock;
    }

    @Override
    public ExpiringContractsResult list(UUID companyId, LocalDate fromDate, LocalDate toDate,
            int page, int pageSize) {
        if (companyId == null || page < 0 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Invalid contract expiry list request");
        }
        var window = new ContractExpiryWindow(fromDate, toDate);
        access.require(companyId, "contract.read");
        String actor = access.currentUserId();
        return transaction.execute(status -> {
            scope.apply(database, actor, companyId);
            var resultPage = contracts.findCurrentVersions(companyId, window.fromDate(),
                    window.toDate(), page, pageSize);
            long missingDates = contracts.countCurrentContractsWithoutEndDate(companyId);
            int totalPages = Math.toIntExact((resultPage.totalItems() + pageSize - 1) / pageSize);
            return new ExpiringContractsResult(companyId, fromDate, toDate,
                    LocalDate.now(clock), BUSINESS_ZONE.getId(), resultPage.items(), page, pageSize,
                    resultPage.totalItems(), totalPages, missingDates);
        });
    }
}
