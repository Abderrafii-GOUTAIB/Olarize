package com.olarize.assistant.application.service;

import com.olarize.assistant.application.exception.ConversationNotFound;
import com.olarize.assistant.application.port.in.ContractExpiryAssistantUseCase;
import com.olarize.assistant.application.port.in.ExpiryAnswer;
import com.olarize.assistant.application.port.out.ConversationPort;
import com.olarize.assistant.application.port.out.ExpiryIntentPort;
import com.olarize.assistant.domain.model.Conversation;
import com.olarize.assistant.domain.model.ExpiryAnswerStatus;
import com.olarize.assistant.domain.model.ExpiryWindow;
import com.olarize.employment.application.port.in.ListExpiringContractsUseCase;
import com.olarize.identity.application.port.in.CompanyAccessUseCase;
import com.olarize.platform.api.DatabaseScopeSigner;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ContractExpiryAssistantService implements ContractExpiryAssistantUseCase {
    private static final Logger LOG = LoggerFactory.getLogger(ContractExpiryAssistantService.class);
    private final CompanyAccessUseCase access;
    private final ConversationPort conversations;
    private final ExpiryIntentPort provider;
    private final ListExpiringContractsUseCase contracts;
    private final DatabaseScopeSigner scope;
    private final JdbcTemplate database;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public ContractExpiryAssistantService(CompanyAccessUseCase access, ConversationPort conversations,
            ExpiryIntentPort provider, ListExpiringContractsUseCase contracts, DatabaseScopeSigner scope,
            JdbcTemplate database, PlatformTransactionManager transactions, @Qualifier("assistantClock") Clock clock) {
        this.access = access; this.conversations = conversations; this.provider = provider; this.contracts = contracts;
        this.scope = scope; this.database = database; this.transaction = new TransactionTemplate(transactions); this.clock = clock;
    }
    @Override public Conversation create(UUID companyId) {
        access.require(companyId, "contract.read");
        String actor = access.currentUserId();
        var now = clock.instant();
        return transaction.execute(status -> {
            scope.apply(database, actor, companyId);
            return conversations.save(new Conversation(UUID.randomUUID(), companyId, actor, now, now.plus(Duration.ofHours(24))));
        });
    }
    private void requireOwnedConversation(UUID companyId, UUID conversationId) {
        access.require(companyId, "contract.read");
        String actor = access.currentUserId();
        transaction.executeWithoutResult(status -> {
            scope.apply(database, actor, companyId);
            var conversation = conversations.find(companyId, conversationId).orElseThrow(ConversationNotFound::new);
            if (!actor.equals(conversation.ownerUserId()) || !conversation.expiresAt().isAfter(clock.instant()))
                throw new ConversationNotFound();
        });
    }
    @Override public ExpiryAnswer ask(UUID companyId, UUID conversationId, String question, int page, int pageSize) {
        if (companyId == null || conversationId == null || question == null || question.isBlank() || question.length() > 1000
                || page < 0 || pageSize < 1 || pageSize > 100 || (long) page * pageSize > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Invalid expiry request");
        long started = System.nanoTime();
        String outcome = "FAILURE";
        try {
            requireOwnedConversation(companyId, conversationId);
            var call = provider.plan(question);
            if ("clarify".equals(call.toolName()) && call.arguments().isEmpty()) {
                outcome = "CLARIFICATION_REQUIRED";
                return new ExpiryAnswer(conversationId, ExpiryAnswerStatus.CLARIFICATION_REQUIRED,
                        "Specify the month and year, or use the date filters in the contract expiry API.", null);
            }
            if ("refuse".equals(call.toolName()) && call.arguments().isEmpty()) {
                outcome = "REFUSED";
                return new ExpiryAnswer(conversationId, ExpiryAnswerStatus.REFUSED,
                        "Only the contract expiry query is supported.", null);
            }
            var window = ExpiryWindow.resolve(call, clock);
            // Planning never holds a database connection. Recheck ownership/permission after the provider returns.
            requireOwnedConversation(companyId, conversationId);
            var result = contracts.list(companyId, window.fromDate(), window.toDate(), page, pageSize);
            boolean empty = result.totalItems() == 0;
            outcome = empty ? "EMPTY" : "RESULTS";
            return new ExpiryAnswer(conversationId, empty ? ExpiryAnswerStatus.EMPTY : ExpiryAnswerStatus.RESULTS,
                    empty ? "No recorded contract expiries match this date window." : "Recorded contract expiries for the selected date window.", result);
        } finally {
            LOG.info("assistant.expiry.request outcome={} durationMs={}", outcome, (System.nanoTime() - started) / 1_000_000);
        }
    }
}
