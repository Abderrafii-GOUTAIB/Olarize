package com.olarize.employment.infrastructure.out.persistence.adapter;

import com.olarize.employment.infrastructure.out.persistence.repository.EmployeeJobJpaRepository;

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

    private final EmployeeJobJpaRepository jobs;
    EmployeeDirectoryPersistenceAdapter(EmployeeDirectoryJpaRepository employees,
            EmployeeJobJpaRepository jobs) {
        this.employees = employees;
        this.jobs = jobs;
    }
    private EmployeeRecord map(EmployeeDirectoryJpaRepository.EmployeeProjection row, LocalDate asOf) {
        // Upcoming employees expose their initial recorded job, never a later scheduled change.
        LocalDate date = asOf.isBefore(row.getEpisode().getStartDate()) ? row.getEpisode().getStartDate() : asOf;
        var job = jobs.findAt(row.getEmployee().getCompanyId(), row.getEpisode().getId(), date, PageRequest.of(0, 1));
        boolean hasHistory = !job.isEmpty() || jobs.existsByCompanyIdAndEpisodeId(row.getEmployee().getCompanyId(), row.getEpisode().getId());
        return EmployeeDirectoryPersistenceMapper.toModel(row, asOf, job.isEmpty() ? null : job.getFirst(), hasHistory);
    }

    @Override
    public EmployeeDirectoryPage find(UUID companyId, LocalDate asOf, EmployeeDirectoryFilter filter) {
        var page = employees.findDirectory(companyId, asOf, filter.searchPattern(), filter.status().name(),
                PageRequest.of(filter.page(), filter.pageSize()));
        return new EmployeeDirectoryPage(page.getContent().stream()
                .map(row -> map(row, asOf)).toList(), page.getTotalElements());
    }

    @Override
    public Optional<EmployeeRecord> findById(UUID companyId, UUID employeeId, LocalDate asOf) {
        return employees.findDetail(companyId, employeeId, asOf)
                .map(row -> map(row, asOf));
    }
}
