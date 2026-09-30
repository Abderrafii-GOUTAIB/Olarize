package com.olarize.identity.infrastructure.in.web.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class ConfirmMembershipChangeRequestDto {
    @NotBlank @Size(max = 8192) private String previewToken;
    @NotNull @PositiveOrZero private Long expectedTargetVersion;
    @NotNull
    @Schema(description = "A new UUID for this change. Reuse the same UUID only when retrying this exact confirmation, so the change is applied once.")
    private UUID idempotencyKey;
    @NotNull private Boolean confirm;

    public String getPreviewToken() { return previewToken; }
    public void setPreviewToken(String previewToken) { this.previewToken = previewToken; }
    public Long getExpectedTargetVersion() { return expectedTargetVersion; }
    public void setExpectedTargetVersion(Long expectedTargetVersion) { this.expectedTargetVersion = expectedTargetVersion; }
    public UUID getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(UUID idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public Boolean getConfirm() { return confirm; }
    public void setConfirm(Boolean confirm) { this.confirm = confirm; }
    @JsonAnySetter public void rejectUnknown(String property, Object value) { throw new IllegalArgumentException("Unknown request field"); }
}
