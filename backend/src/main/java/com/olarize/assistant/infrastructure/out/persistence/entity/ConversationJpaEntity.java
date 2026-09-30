package com.olarize.assistant.infrastructure.out.persistence.entity;

import com.olarize.assistant.domain.model.Conversation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "assistant_conversation")
public class ConversationJpaEntity {
    @Id private UUID id;
    @Column(name = "company_id", nullable = false) private UUID companyId;
    @Column(name = "owner_user_id", nullable = false) private String ownerUserId;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    protected ConversationJpaEntity() { }
    public ConversationJpaEntity(Conversation value) {
        id = value.id(); companyId = value.companyId(); ownerUserId = value.ownerUserId();
        createdAt = value.createdAt(); expiresAt = value.expiresAt();
    }
    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getOwnerUserId() { return ownerUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
}
