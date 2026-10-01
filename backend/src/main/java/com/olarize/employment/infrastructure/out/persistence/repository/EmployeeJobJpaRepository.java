package com.olarize.employment.infrastructure.out.persistence.repository;

import com.olarize.employment.infrastructure.out.persistence.entity.EmployeeJobJpaEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;

public interface EmployeeJobJpaRepository extends JpaRepository<EmployeeJobJpaEntity, UUID> {
    boolean existsByCompanyIdAndEpisodeId(UUID companyId, UUID episodeId);
    List<EmployeeJobJpaEntity> findByCompanyIdAndEpisodeId(UUID companyId, UUID episodeId);
    @Query("select j from EmployeeJobJpaEntity j where j.companyId = :companyId and j.episodeId = :episodeId "
            + "and j.effectiveDate <= :asOf order by j.effectiveDate desc, j.sourceVersion desc")
    List<EmployeeJobJpaEntity> findAt(UUID companyId, UUID episodeId, LocalDate asOf, Pageable pageable);
}
