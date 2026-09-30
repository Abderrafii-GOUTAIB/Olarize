package com.olarize.employment.infrastructure.out.persistence.adapter;

import com.olarize.employment.application.port.in.EmployeeDirectoryPage;
import com.olarize.employment.application.port.in.EmployeeRecord;
import com.olarize.employment.application.port.out.EmployeeDirectoryPort;
import com.olarize.employment.domain.model.EmployeeDirectoryFilter;
import com.olarize.employment.infrastructure.out.persistence.mapper.EmployeeDirectoryPersistenceMapper;
import com.olarize.employment.infrastructure.out.persistence.repository.EmployeeDirectoryJpaRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
class EmployeeDirectoryPersistenceAdapter implements EmployeeDirectoryPort {
    private final EmployeeDirectoryJpaRepository employees;

    EmployeeDirectoryPersistenceAdapter(EmployeeDirectoryJpaRepository employees) { this.employees = employees; }

    @Override
    public EmployeeDirectoryPage find(UUID companyId, LocalDate asOf, EmployeeDirectoryFilter filter) {
        var page = employees.findDirectory(companyId, asOf, filter.searchPattern(), filter.status().name(),
                PageRequest.of(filter.page(), filter.pageSize()));
        return new EmployeeDirectoryPage(page.getContent().stream()
                .map(row -> EmployeeDirectoryPersistenceMapper.toModel(row, asOf)).toList(), page.getTotalElements());
    }

    @Override
    public Optional<EmployeeRecord> findById(UUID companyId, UUID employeeId, LocalDate asOf) {
        return employees.findDetail(companyId, employeeId, asOf)
                .map(row -> EmployeeDirectoryPersistenceMapper.toModel(row, asOf));
    }
}
