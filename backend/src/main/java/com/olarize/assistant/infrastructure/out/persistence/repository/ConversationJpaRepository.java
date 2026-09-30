package com.olarize.assistant.infrastructure.out.persistence.repository;

import com.olarize.assistant.infrastructure.out.persistence.entity.ConversationJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationJpaRepository extends JpaRepository<ConversationJpaEntity, UUID> {
    Optional<ConversationJpaEntity> findByCompanyIdAndId(UUID companyId, UUID id);
}
