package com.olarize.identity.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.olarize.identity.domain.model.MembershipRoleBundle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class MembershipChangePreviewRequest {
    @NotBlank @Size(max = 255) private String targetSubject;
    private MembershipRoleBundle roleBundle;
    @NotBlank @Size(max = 1000) private String reason;

    public String getTargetSubject() { return targetSubject; }
    public void setTargetSubject(String targetSubject) { this.targetSubject = targetSubject; }
    public MembershipRoleBundle getRoleBundle() { return roleBundle; }
    public void setRoleBundle(MembershipRoleBundle roleBundle) { this.roleBundle = roleBundle; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    @JsonAnySetter public void rejectUnknown(String property, Object value) { throw new IllegalArgumentException("Unknown request field"); }
}
