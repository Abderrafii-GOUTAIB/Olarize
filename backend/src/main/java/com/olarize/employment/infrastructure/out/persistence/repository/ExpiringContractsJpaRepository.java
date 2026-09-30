package com.olarize.employment.infrastructure.out.persistence.repository;

import com.olarize.employment.infrastructure.out.persistence.entity.EmploymentContractJpaEntity;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpiringContractsJpaRepository extends JpaRepository<EmploymentContractJpaEntity, UUID> {
    @Query(value = "select c.id as contractId, c.employeeId as employeeId, e.name as employeeName, "
            + "e.reference as employeeReference, v.version as contractVersion, v.startDate as startDate, "
            + "v.endDate as endDate from EmploymentContractJpaEntity c, "
            + "EmploymentContractVersionJpaEntity v, EmployeeJpaEntity e, EmploymentEpisodeJpaEntity episode "
            + "where c.companyId = :companyId and v.contractId = c.id and v.companyId = c.companyId "
            + "and v.version = c.currentVersion and v.endDate between :fromDate and :toDate "
            + "and e.id = c.employeeId and e.companyId = c.companyId "
            + "and episode.id = c.episodeId and episode.employeeId = c.employeeId "
            + "and episode.companyId = c.companyId order by v.endDate, c.id",
            countQuery = "select count(c) from EmploymentContractJpaEntity c, "
                    + "EmploymentContractVersionJpaEntity v, EmploymentEpisodeJpaEntity episode "
                    + "where c.companyId = :companyId and v.contractId = c.id and v.companyId = c.companyId "
                    + "and v.version = c.currentVersion and v.endDate between :fromDate and :toDate "
                    + "and episode.id = c.episodeId and episode.employeeId = c.employeeId "
                    + "and episode.companyId = c.companyId")
    Page<ExpiringContractProjection> findCurrentVersions(@Param("companyId") UUID companyId,
            @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate, Pageable pageable);

    @Query("select count(c) from EmploymentContractJpaEntity c, EmploymentContractVersionJpaEntity v "
            + "where c.companyId = :companyId and v.contractId = c.id and v.companyId = c.companyId "
            + "and v.version = c.currentVersion and v.endDate is null")
    long countCurrentContractsWithoutEndDate(@Param("companyId") UUID companyId);

    interface ExpiringContractProjection {
        UUID getContractId();
        UUID getEmployeeId();
        String getEmployeeName();
        String getEmployeeReference();
        long getContractVersion();
        LocalDate getStartDate();
        LocalDate getEndDate();
    }
}
