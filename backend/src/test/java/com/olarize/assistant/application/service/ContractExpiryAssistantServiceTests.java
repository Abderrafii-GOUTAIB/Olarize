package com.olarize.assistant.application.service;

import com.olarize.assistant.application.exception.ConversationNotFound;
import com.olarize.assistant.application.port.out.ConversationPort;
import com.olarize.assistant.application.port.out.ExpiryIntentPort;
import com.olarize.assistant.domain.model.Conversation;
import com.olarize.assistant.domain.model.ExpiryToolCall;
import com.olarize.assistant.domain.model.ExpiryAnswerStatus;
import com.olarize.employment.application.port.in.ListExpiringContractsUseCase;
import com.olarize.identity.application.port.in.CompanyAccessUseCase;
import com.olarize.platform.api.DatabaseScopeSigner;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ContractExpiryAssistantServiceTests {
    private final CompanyAccessUseCase access = mock(CompanyAccessUseCase.class);
    private final ConversationPort conversations = mock(ConversationPort.class);
    private final ExpiryIntentPort provider = mock(ExpiryIntentPort.class);
    private final ListExpiringContractsUseCase contracts = mock(ListExpiringContractsUseCase.class);
    private final DatabaseScopeSigner scope = mock(DatabaseScopeSigner.class);
    private final JdbcTemplate database = mock(JdbcTemplate.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final UUID company = UUID.randomUUID();
    private final UUID conversation = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-09-23T12:00:00Z");
    private ContractExpiryAssistantService service(String owner) {
        when(access.currentUserId()).thenReturn("actor");
        when(transactions.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        when(conversations.find(company, conversation)).thenReturn(Optional.of(new Conversation(conversation, company, owner, now, now.plusSeconds(3600))));
        return new ContractExpiryAssistantService(access, conversations, provider, contracts, scope, database, transactions,
                Clock.fixed(now, ZoneOffset.UTC));
    }
    @Test void deniedPermissionCannotCallProviderOrDatabase() {
        var service = service("actor");
        doThrow(new AccessDeniedException("denied")).when(access).require(company, "contract.read");
        assertThatThrownBy(() -> service.ask(company, conversation, "question", 0, 25)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(provider, contracts, scope, database);
    }
    @Test void otherOwnerCannotReachProviderAndClarificationDoesNotQuery() {
        var service = service("other");
        assertThatThrownBy(() -> service.ask(company, conversation, "question", 0, 25)).isInstanceOf(ConversationNotFound.class);
        verifyNoInteractions(provider, contracts);
        var ownedService = service("actor");
        when(provider.plan("question")).thenReturn(new ExpiryToolCall("clarify", Map.of()));
        assertThat(ownedService.ask(company, conversation, "question", 0, 25).status()).isEqualTo(ExpiryAnswerStatus.CLARIFICATION_REQUIRED);
        verifyNoInteractions(contracts);
    }
    @Test void untrustedModelIdentityOverrideIsRejectedBeforeQuery() {
        var service = service("actor");
        when(provider.plan("question")).thenReturn(new ExpiryToolCall("listExpiringContracts", Map.of("period", "NEXT_MONTH", "subject", "administrator")));
        assertThatThrownBy(() -> service.ask(company, conversation, "question", 0, 25)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(contracts);
    }
}
