package com.olarize.identity.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class ConfirmMembershipChangeRequest {
    @NotBlank @Size(max = 8192) private String previewToken;
    @NotNull @PositiveOrZero private Long expectedTargetVersion;
    @NotNull private UUID idempotencyKey;
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
