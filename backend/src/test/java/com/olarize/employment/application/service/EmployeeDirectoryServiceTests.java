package com.olarize.employment.application.service;

import com.olarize.employment.application.exception.EmployeeNotFound;
import com.olarize.employment.application.port.in.EmployeeDirectoryPage;
import com.olarize.employment.application.port.out.EmployeeDirectoryPort;
import com.olarize.employment.domain.model.EmployeeDirectoryFilter;
import com.olarize.employment.domain.model.EmployeeDirectoryStatus;
import com.olarize.identity.application.port.in.CompanyAccessUseCase;
import com.olarize.platform.api.DatabaseScopeSigner;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmployeeDirectoryServiceTests {
    private final CompanyAccessUseCase access = mock(CompanyAccessUseCase.class);
    private final DatabaseScopeSigner scope = mock(DatabaseScopeSigner.class);
    private final JdbcTemplate database = mock(JdbcTemplate.class);
    private final EmployeeDirectoryPort records = mock(EmployeeDirectoryPort.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final UUID companyId = UUID.randomUUID();
    private final EmployeeDirectoryFilter filter = new EmployeeDirectoryFilter(null, EmployeeDirectoryStatus.CURRENT, 0, 25);
    private final EmployeeDirectoryService service = new EmployeeDirectoryService(access, scope, database, records,
            transactions, Clock.fixed(Instant.parse("2026-09-30T23:30:00Z"), ZoneOffset.UTC));

    @Test void deniedMembershipNeverQueriesPersistenceOrStartsTransaction() {
        when(access.require(companyId, "employee.read")).thenThrow(new AccessDeniedException("denied"));
        assertThatThrownBy(() -> service.list(companyId, null, filter)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(records, scope, transactions);
    }

    @Test void resolvesCasablancaDateAndEstablishesScopeBeforeQuerying() {
        when(access.currentUserId()).thenReturn("synthetic-reader");
        when(transactions.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());
        var date = LocalDate.of(2026, 10, 1);
        when(records.find(companyId, date, filter)).thenReturn(new EmployeeDirectoryPage(List.of(), 0));
        var result = service.list(companyId, null, filter);
        assertThat(result.asOf()).isEqualTo(date);
        assertThat(result.timeZone()).isEqualTo("Africa/Casablanca");
        var order = inOrder(scope, records);
        order.verify(scope).apply(database, "synthetic-reader", companyId);
        order.verify(records).find(companyId, date, filter);
    }

    @Test void scopedMissingDetailIsExplicitAndDoesNotBecomeAnEmptyRecord() {
        when(access.currentUserId()).thenReturn("synthetic-reader");
        when(transactions.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());
        var employeeId = UUID.randomUUID();
        var date = LocalDate.of(2026, 9, 30);
        when(records.findById(companyId, employeeId, date)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.detail(companyId, employeeId, date)).isInstanceOf(EmployeeNotFound.class);
        verify(access).require(companyId, "employee.read");
    }
}
