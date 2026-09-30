package com.olarize.assistant.application.port.in;

import com.olarize.assistant.domain.model.Conversation;
import java.util.UUID;

public interface ContractExpiryAssistantUseCase {
    Conversation create(UUID companyId);
    ExpiryAnswer ask(UUID companyId, UUID conversationId, String question, int page, int pageSize);
}
