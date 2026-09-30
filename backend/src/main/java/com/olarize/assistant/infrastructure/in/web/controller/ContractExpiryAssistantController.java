package com.olarize.assistant.infrastructure.in.web.controller;

import com.olarize.assistant.application.exception.AssistantUnavailable;
import com.olarize.assistant.application.exception.ConversationNotFound;
import com.olarize.assistant.application.port.in.ContractExpiryAssistantUseCase;
import com.olarize.assistant.infrastructure.in.web.dto.request.ContractExpiryQuestionRequestDto;
import com.olarize.assistant.infrastructure.in.web.dto.response.ConversationResponseDto;
import com.olarize.assistant.infrastructure.in.web.dto.response.ContractExpiryAnswerResponseDto;
import com.olarize.assistant.infrastructure.in.web.mapper.ContractExpiryAssistantWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/assistant/conversations")
@SecurityRequirement(name = "keycloak", scopes = "profile.read")
class ContractExpiryAssistantController {
    private final ContractExpiryAssistantUseCase assistant;
    ContractExpiryAssistantController(ContractExpiryAssistantUseCase assistant) { this.assistant = assistant; }

    @PostMapping
    @Operation(summary = "Start an owned expiry conversation", description = "Creates metadata owned by the authenticated user in the selected company. "
            + "Requires current contract.read and profile.read. Expires after 24 hours; stores no question, transcript or HR result. No employment facts are changed.",
            responses = {@ApiResponse(responseCode = "201", description = "Copy the new conversationId into the expiry request"),
                @ApiResponse(responseCode = "400", description = "Invalid company ID"),
                @ApiResponse(responseCode = "401", description = "Valid Keycloak access token required"),
                @ApiResponse(responseCode = "403", description = "Current contract.read required"),
                @ApiResponse(responseCode = "500", description = "Conversation storage unavailable; problem includes requestId")})
    ResponseEntity<ConversationResponseDto> create(@PathVariable UUID companyId) {
        try { return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(ContractExpiryAssistantWebMapper.response(assistant.create(companyId))); }
        catch (AccessDeniedException denied) { throw new ResponseStatusException(HttpStatus.FORBIDDEN); }
    }

    @PostMapping("/{conversationId}/contract-expiry-requests")
    @Operation(summary = "Ask for recorded contract expiries", description = "One read-only listExpiringContracts tool using current contract.read and profile.read. "
            + "Only the conversation owner can ask. NEXT_MONTH uses Africa/Casablanca; dates and actual records come from the backend service. "
            + "Local profile uses reviewed English, French and Arabic synthetic questions, not a live language model. Unknown wording clarifies without querying. "
            + "Use the existing GET contracts/expiring API with fromDate/toDate as structured fallback. No confirmation or HR mutation.",
            responses = {@ApiResponse(responseCode = "200", description = "RESULTS, EMPTY, CLARIFICATION_REQUIRED or REFUSED with actual service result when queried"),
                @ApiResponse(responseCode = "400", description = "Invalid question, pagination or untrusted tool arguments"),
                @ApiResponse(responseCode = "401", description = "Valid Keycloak access token required"),
                @ApiResponse(responseCode = "403", description = "Current contract.read required"),
                @ApiResponse(responseCode = "404", description = "Conversation absent, expired or owned by another user/company"),
                @ApiResponse(responseCode = "503", description = "Provider disabled, saturated, failed or timed out; use GET contracts/expiring"),
                @ApiResponse(responseCode = "500", description = "Tool/database unavailable; use requestId and structured API fallback")})
    ResponseEntity<ContractExpiryAnswerResponseDto> ask(@PathVariable UUID companyId,
            @Parameter(description = "Copy the ID returned by start conversation") @PathVariable UUID conversationId,
            @Valid @RequestBody ContractExpiryQuestionRequestDto input) {
        try { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ContractExpiryAssistantWebMapper.response(companyId,
                    assistant.ask(companyId, conversationId, input.question(), input.page() == null ? 0 : input.page(),
                            input.pageSize() == null ? 25 : input.pageSize()))); }
        catch (AccessDeniedException denied) { throw new ResponseStatusException(HttpStatus.FORBIDDEN); }
        catch (ConversationNotFound missing) { throw new ResponseStatusException(HttpStatus.NOT_FOUND); }
        catch (AssistantUnavailable unavailable) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE); }
        catch (IllegalArgumentException invalid) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST); }
    }
}
