package com.olarize.employment.adapter.out.persistence;

import com.olarize.employment.domain.model.EmployeeCreation;

final class EmployeePersistenceMapper {
    private EmployeePersistenceMapper() { }

    static EmployeeCreation facts(EmployeeJpaEntity employee, EmploymentEpisodeJpaEntity episode) {
        return EmployeeCreation.of(employee.reference, employee.name, episode.startDate,
                employee.jobTitle, employee.department, employee.site, employee.workEmail, employee.workPhone);
    }
}
