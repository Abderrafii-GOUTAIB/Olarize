package com.olarize.assistant.infrastructure.out.persistence.mapper;

import com.olarize.assistant.domain.model.Conversation;
import com.olarize.assistant.infrastructure.out.persistence.entity.ConversationJpaEntity;

public final class ConversationPersistenceMapper {
    private ConversationPersistenceMapper() { }
    public static Conversation model(ConversationJpaEntity entity) {
        return new Conversation(entity.getId(), entity.getCompanyId(), entity.getOwnerUserId(), entity.getCreatedAt(), entity.getExpiresAt());
    }
}
