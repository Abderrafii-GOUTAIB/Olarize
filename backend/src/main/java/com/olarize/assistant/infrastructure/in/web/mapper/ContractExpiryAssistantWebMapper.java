package com.olarize.assistant.infrastructure.in.web.mapper;

import com.olarize.assistant.application.port.in.ExpiryAnswer;
import com.olarize.assistant.domain.model.Conversation;
import com.olarize.assistant.infrastructure.in.web.dto.response.ConversationResponseDto;
import com.olarize.assistant.infrastructure.in.web.dto.response.ContractExpiryAnswerResponseDto;
import com.olarize.assistant.infrastructure.in.web.dto.response.ExpiryContractResponseDto;
import com.olarize.assistant.infrastructure.in.web.dto.response.ExpiryResultResponseDto;
import java.util.UUID;

public final class ContractExpiryAssistantWebMapper {
    private ContractExpiryAssistantWebMapper() { }
    public static ConversationResponseDto response(Conversation value) {
        return new ConversationResponseDto(value.id(), value.companyId(), value.expiresAt());
    }
    public static ContractExpiryAnswerResponseDto response(UUID companyId, ExpiryAnswer answer) {
        var value = answer.result();
        ExpiryResultResponseDto result = value == null ? null : new ExpiryResultResponseDto(value.companyId(),
                value.fromDate(), value.toDate(), value.asOf(), value.timeZone(), value.items().stream().map(item ->
                        new ExpiryContractResponseDto(item.contractId(), item.contractVersion(), item.employeeId(),
                                item.employeeName(), item.employeeReference(), item.startDate(), item.endDate())).toList(),
                value.page(), value.pageSize(), value.totalItems(), value.totalPages(), value.currentContractsWithoutRecordedEndDate());
        return new ContractExpiryAnswerResponseDto(answer.conversationId(), answer.status(), answer.message(), result,
                "/api/v1/companies/" + companyId + "/contracts/expiring");
    }
}
