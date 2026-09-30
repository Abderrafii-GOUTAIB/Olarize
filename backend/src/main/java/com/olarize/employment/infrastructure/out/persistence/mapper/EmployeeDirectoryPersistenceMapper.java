package com.olarize.employment.infrastructure.out.persistence.mapper;

import com.olarize.employment.application.port.in.EmployeeRecord;
import com.olarize.employment.domain.model.EmploymentStatus;
import com.olarize.employment.infrastructure.out.persistence.repository.EmployeeDirectoryJpaRepository.EmployeeProjection;
import java.time.LocalDate;

public final class EmployeeDirectoryPersistenceMapper {
    private EmployeeDirectoryPersistenceMapper() { }

    public static EmployeeRecord toModel(EmployeeProjection row, LocalDate asOf) {
        var employee = row.getEmployee();
        var episode = row.getEpisode();
        return new EmployeeRecord(employee.getId(), employee.getCompanyId(), employee.getReference(), employee.getName(),
                employee.getJobTitle(), employee.getDepartment(), employee.getSite(), employee.getWorkEmail(),
                employee.getWorkPhone(), employee.getVersion(), employee.getRecordedAt(), episode.getId(),
                episode.getStartDate(), episode.getLastEmployedDay(), episode.getVersion(),
                EmploymentStatus.asOf(episode.getStartDate(), episode.getLastEmployedDay(), asOf));
    }
}
