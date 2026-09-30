package com.olarize.assistant.infrastructure.out.persistence.adapter;

import com.olarize.assistant.application.port.out.ConversationPort;
import com.olarize.assistant.domain.model.Conversation;
import com.olarize.assistant.infrastructure.out.persistence.entity.ConversationJpaEntity;
import com.olarize.assistant.infrastructure.out.persistence.mapper.ConversationPersistenceMapper;
import com.olarize.assistant.infrastructure.out.persistence.repository.ConversationJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class ConversationPersistenceAdapter implements ConversationPort {
    private final ConversationJpaRepository repository;
    ConversationPersistenceAdapter(ConversationJpaRepository repository) { this.repository = repository; }
    @Override public Conversation save(Conversation conversation) {
        return ConversationPersistenceMapper.model(repository.saveAndFlush(new ConversationJpaEntity(conversation)));
    }
    @Override public Optional<Conversation> find(UUID companyId, UUID id) {
        return repository.findByCompanyIdAndId(companyId, id).map(ConversationPersistenceMapper::model);
    }
}
