package com.olarize.assistant.application.port.out;

import com.olarize.assistant.domain.model.Conversation;
import java.util.Optional;
import java.util.UUID;

public interface ConversationPort {
    Conversation save(Conversation conversation);
    Optional<Conversation> find(UUID companyId, UUID id);
}
