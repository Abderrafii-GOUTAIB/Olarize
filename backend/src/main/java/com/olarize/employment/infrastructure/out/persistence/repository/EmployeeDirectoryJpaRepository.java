package com.olarize.employment.infrastructure.out.persistence.repository;

import com.olarize.employment.infrastructure.out.persistence.entity.EmployeeJpaEntity;
import com.olarize.employment.infrastructure.out.persistence.entity.EmploymentEpisodeJpaEntity;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeDirectoryJpaRepository extends JpaRepository<EmployeeJpaEntity, UUID> {
    String SELECTED_EPISODE = " from EmployeeJpaEntity e, EmploymentEpisodeJpaEntity episode "
            + "where e.companyId = :companyId and episode.companyId = e.companyId and episode.employeeId = e.id "
            + "and not exists (select other.id from EmploymentEpisodeJpaEntity other "
            + "where other.companyId = e.companyId and other.employeeId = e.id and ("
            + "(episode.startDate <= :asOf and other.startDate <= :asOf and "
            + "(other.startDate > episode.startDate or (other.startDate = episode.startDate and other.id > episode.id))) "
            + "or (episode.startDate > :asOf and (other.startDate <= :asOf or other.startDate < episode.startDate "
            + "or (other.startDate = episode.startDate and other.id > episode.id))))) ";
    String FILTER = "and (lower(e.name) like :search escape '!' or lower(e.reference) like :search escape '!') "
            + "and (:status = 'ALL' or (:status = 'UPCOMING' and episode.startDate > :asOf) "
            + "or (:status = 'CURRENT' and episode.startDate <= :asOf "
            + "and (episode.lastEmployedDay is null or episode.lastEmployedDay >= :asOf)) "
            + "or (:status = 'ENDED' and episode.startDate <= :asOf and episode.lastEmployedDay < :asOf)) ";

    @Query(value = "select e as employee, episode as episode" + SELECTED_EPISODE + FILTER
            + "order by e.normalizedReference, e.id", countQuery = "select count(e)" + SELECTED_EPISODE + FILTER)
    Page<EmployeeProjection> findDirectory(@Param("companyId") UUID companyId, @Param("asOf") LocalDate asOf,
            @Param("search") String search, @Param("status") String status, Pageable pageable);

    @Query("select e as employee, episode as episode" + SELECTED_EPISODE + "and e.id = :employeeId")
    Optional<EmployeeProjection> findDetail(@Param("companyId") UUID companyId,
            @Param("employeeId") UUID employeeId, @Param("asOf") LocalDate asOf);

    interface EmployeeProjection {
        EmployeeJpaEntity getEmployee();
        EmploymentEpisodeJpaEntity getEpisode();
    }
}
