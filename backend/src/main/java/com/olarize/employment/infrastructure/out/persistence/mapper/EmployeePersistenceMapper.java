package com.olarize.employment.infrastructure.out.persistence.mapper;

import com.olarize.employment.infrastructure.out.persistence.entity.EmployeeJpaEntity;
import com.olarize.employment.infrastructure.out.persistence.entity.EmploymentEpisodeJpaEntity;

import com.olarize.employment.domain.model.EmployeeCreation;

public final class EmployeePersistenceMapper {
    private EmployeePersistenceMapper() { }

    public static EmployeeCreation facts(EmployeeJpaEntity employee, EmploymentEpisodeJpaEntity episode) {
        return EmployeeCreation.of(employee.getReference(), employee.getName(), episode.getStartDate(),
                employee.getJobTitle(), employee.getDepartment(), employee.getSite(), employee.getWorkEmail(), employee.getWorkPhone());
    }
}
