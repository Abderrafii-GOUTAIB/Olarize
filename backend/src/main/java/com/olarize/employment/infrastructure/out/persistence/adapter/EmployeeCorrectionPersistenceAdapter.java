package com.olarize.employment.infrastructure.out.persistence.adapter;

import com.olarize.employment.infrastructure.out.persistence.entity.EmployeeJobJpaEntity;
import com.olarize.employment.infrastructure.out.persistence.repository.EmployeeJobJpaRepository;

import com.olarize.employment.application.exception.EmployeeCorrectionConflict;
import com.olarize.employment.application.exception.EmployeeNotFound;
import com.olarize.employment.application.port.in.CorrectEmployeeUseCase.Result;
import com.olarize.employment.application.port.out.EmployeeCorrectionPort;
import com.olarize.employment.domain.model.EmployeeCorrection;
import com.olarize.employment.infrastructure.out.persistence.mapper.EmployeeCorrectionPersistenceMapper;
import com.olarize.employment.infrastructure.out.persistence.mapper.EmployeePersistenceMapper;
import com.olarize.employment.infrastructure.out.persistence.repository.EmployeeJpaRepository;
import com.olarize.employment.infrastructure.out.persistence.repository.EmployeeVersionJpaRepository;
import com.olarize.employment.infrastructure.out.persistence.repository.EmploymentEpisodeJpaRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class EmployeeCorrectionPersistenceAdapter implements EmployeeCorrectionPort {
    private final EmployeeJpaRepository employees;
    private final EmploymentEpisodeJpaRepository episodes;
    private final EmployeeVersionJpaRepository versions;
    private final JdbcTemplate database;
    private final EmployeeJobJpaRepository jobs;

    EmployeeCorrectionPersistenceAdapter(EmployeeJpaRepository employees, EmploymentEpisodeJpaRepository episodes,
            EmployeeVersionJpaRepository versions, JdbcTemplate database,
            EmployeeJobJpaRepository jobs) {
        this.employees = employees;
        this.episodes = episodes;
        this.versions = versions;
        this.database = database;
        this.jobs = jobs;
    }
    @Override public void serializeRetry(UUID companyId, String actor, UUID key) {
        // PostgreSQL transaction lock closes equal-key races; all CRUD uses JPA.
        database.queryForObject("select pg_advisory_xact_lock(hashtextextended(?, 0))", Object.class,
                companyId + ":" + actor + ":EMPLOYEE_CORRECT:" + key);
    }
    @Override public Optional<Receipt> findRetry(UUID companyId, String actor, UUID key) {
        return versions.findByCompanyIdAndActorSubjectAndIdempotencyKey(companyId, actor, key)
                .map(v -> new Receipt(v.getPayloadHash(), EmployeeCorrectionPersistenceMapper.result(v)));
    }
    @Override public Result correct(UUID companyId, UUID employeeId, UUID episodeId, long version,
            long episodeVersion, EmployeeCorrection correction, String actor, UUID key, String hash,
            String correlationId, Instant time) {
        // PostgreSQL timestamps store microseconds. Normalize before building the initial
        // response so an exact retry loaded from persistence returns the identical instant.
        time = time.truncatedTo(ChronoUnit.MICROS);
        var employee = employees.findByIdAndCompanyId(employeeId, companyId).orElseThrow(EmployeeNotFound::new);
        var episode = episodes.findByIdAndEmployeeIdAndCompanyId(episodeId, employeeId, companyId).orElseThrow(EmployeeNotFound::new);
        if (employee.getVersion() != version || episode.getVersion() != episodeVersion) throw new EmployeeCorrectionConflict();
        var old = EmployeePersistenceMapper.facts(employee, episode);
        var f = correction.facts();
        var jobDates = new java.util.ArrayList<>(jobs.findByCompanyIdAndEpisodeId(companyId, episodeId)
                .stream().map(v -> v.getEffectiveDate()).toList());
        boolean initialJob = old.jobTitle() != null || old.department() != null || old.site() != null;
        if (jobDates.isEmpty() && initialJob) jobDates.add(episode.getStartDate());
        boolean overlap = episodes.findAllByEmployeeIdAndCompanyId(employeeId, companyId).stream()
                .filter(other -> !other.getId().equals(episodeId))
                .anyMatch(other -> (episode.getLastEmployedDay() == null || !other.getStartDate().isAfter(episode.getLastEmployedDay()))
                        && (other.getLastEmployedDay() == null || !other.getLastEmployedDay().isBefore(f.startDate())));
        if (!f.startDate().equals(episode.getStartDate())) {
            var conflicts = EmployeeCorrection.startConflicts(f.startDate(), episode.getLastEmployedDay(), jobDates, overlap);
            if (!conflicts.isEmpty()) throw new EmployeeCorrectionConflict(conflicts);
        }
        if (episode.getLastEmployedDay() != null && correction.effectiveDate().isAfter(episode.getLastEmployedDay())) {
            throw new EmployeeCorrectionConflict(java.util.List.of("LAST_EMPLOYED_DAY"));
        }
        if (version == 1 && versions.findByCompanyIdAndEmployeeIdAndVersion(companyId, employeeId, 1).isEmpty()) {
            versions.save(EmployeeCorrectionPersistenceMapper.snapshot(employee, episode, old, 1, episodeVersion,
                    episode.getStartDate(), initialJob, null, null, null, employee.getRecordedAt(), null, null));
        }
        if (jobs.findByCompanyIdAndEpisodeId(companyId, episodeId).isEmpty() && initialJob) {
            jobs.saveAndFlush(new EmployeeJobJpaEntity(
                    companyId, employeeId, episodeId, 1, episode.getStartDate(), old.jobTitle(), old.department(), old.site()));
        }
        var job = jobs.findAt(companyId, episodeId, correction.effectiveDate(), PageRequest.of(0, 1));
        String priorTitle = job.isEmpty() ? null : job.getFirst().getJobTitle();
        String priorDepartment = job.isEmpty() ? null : job.getFirst().getDepartment();
        String priorSite = job.isEmpty() ? null : job.getFirst().getSite();
        boolean jobChanged = !Objects.equals(priorTitle, f.jobTitle())
                || !Objects.equals(priorDepartment, f.department()) || !Objects.equals(priorSite, f.site());
        employee.correct(f, time);
        employees.saveAndFlush(employee);
        if (!episode.getStartDate().equals(f.startDate())) {
            episode.correctStart(f.startDate(), time);
            episodes.saveAndFlush(episode);
        }
        var snapshot = EmployeeCorrectionPersistenceMapper.snapshot(employee, episode, f, employee.getVersion(),
                episode.getVersion(), correction.effectiveDate(), jobChanged, actor, correction.reason(), correlationId, time, key, hash);
        versions.saveAndFlush(snapshot);
        if (jobChanged) jobs.saveAndFlush(new EmployeeJobJpaEntity(
                companyId, employeeId, episodeId, employee.getVersion(), correction.effectiveDate(), f.jobTitle(), f.department(), f.site()));
        return EmployeeCorrectionPersistenceMapper.result(snapshot);
    }
}
