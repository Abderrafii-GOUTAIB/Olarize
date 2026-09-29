package com.olarize.identity.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.olarize.identity.domain.model.MembershipRoleBundle;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class MembershipChangePreviewRequest {
    @NotBlank @Size(max = 255)
    @Schema(description = "Keycloak ID from the selected membership's userId field. Do not use the logged-in administrator's ID.")
    private String userId;
    private MembershipRoleBundle roleBundle;
    @NotBlank @Size(max = 1000) private String reason;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public MembershipRoleBundle getRoleBundle() { return roleBundle; }
    public void setRoleBundle(MembershipRoleBundle roleBundle) { this.roleBundle = roleBundle; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    @JsonAnySetter public void rejectUnknown(String property, Object value) { throw new IllegalArgumentException("Unknown request field"); }
}
