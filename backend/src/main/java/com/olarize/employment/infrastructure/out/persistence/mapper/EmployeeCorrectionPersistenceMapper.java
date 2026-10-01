package com.olarize.employment.infrastructure.out.persistence.mapper;

import com.olarize.employment.application.port.in.CorrectEmployeeUseCase.Result;
import com.olarize.employment.domain.model.EmployeeCreation;
import com.olarize.employment.infrastructure.out.persistence.entity.EmployeeJpaEntity;
import com.olarize.employment.infrastructure.out.persistence.entity.EmployeeVersionJpaEntity;
import com.olarize.employment.infrastructure.out.persistence.entity.EmploymentEpisodeJpaEntity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class EmployeeCorrectionPersistenceMapper {
    private EmployeeCorrectionPersistenceMapper() { }
    public static EmployeeVersionJpaEntity snapshot(EmployeeJpaEntity employee, EmploymentEpisodeJpaEntity episode,
            EmployeeCreation facts, long version, long episodeVersion, LocalDate effectiveDate, boolean jobChanged,
            String actor, String reason, String correlation, Instant time, UUID key, String hash) {
        return new EmployeeVersionJpaEntity(UUID.randomUUID(), employee.getCompanyId(), employee.getId(), episode.getId(),
                version, episodeVersion, facts.reference(), facts.normalizedReference(), facts.name(), facts.startDate(),
                facts.jobTitle(), facts.department(), facts.site(), facts.workEmail(), facts.workPhone(), effectiveDate,
                jobChanged, actor, reason, correlation, time, key, hash);
    }
    public static EmployeeCreation facts(EmployeeVersionJpaEntity version) {
        return EmployeeCreation.of(version.getReference(), version.getName(), version.getStartDate(),
                version.getJobTitle(), version.getDepartment(), version.getSite(), version.getWorkEmail(), version.getWorkPhone());
    }
    public static Result result(EmployeeVersionJpaEntity version) {
        return new Result(version.getCompanyId(), version.getEmployeeId(), version.getEpisodeId(), version.getVersion(),
                version.getEpisodeVersion(), version.getEffectiveDate(), version.getRecordedAt());
    }
}
