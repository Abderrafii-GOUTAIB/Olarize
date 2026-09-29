package com.olarize.identity.adapter.out.security;

import com.olarize.identity.application.model.MembershipChangePreviewClaims;
import com.olarize.identity.application.port.out.MembershipPreviewTokenPort;
import com.olarize.identity.domain.exception.InvalidMembershipPreview;
import com.olarize.identity.domain.model.MembershipChangeAction;
import com.olarize.identity.domain.model.MembershipRoleBundle;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class MembershipPreviewTokenSigner implements MembershipPreviewTokenPort {
    private final byte[] secret;
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    MembershipPreviewTokenSigner(@Value("${olarize.identity.preview-secret:${random.uuid}-${random.uuid}}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        if (this.secret.length < 32) throw new IllegalArgumentException("Preview signing key must contain at least 32 bytes");
    }

    @Override public String issue(MembershipChangePreviewClaims claims) {
        String permissions = String.join(",", claims.permissions().stream().sorted().toList());
        String[] fields = {"v1", claims.previewId().toString(), claims.actorSubject(), claims.companyId().toString(),
            claims.targetSubject(), claims.action().name(), claims.roleBundle() == null ? "" : claims.roleBundle().name(),
            permissions, Long.toString(claims.actorVersion()), Long.toString(claims.targetVersion()),
            claims.reason(), Long.toString(claims.expiresAt().getEpochSecond())};
        String payload = Arrays.stream(fields).map(MembershipPreviewTokenSigner::encodeField)
                .collect(java.util.stream.Collectors.joining("|"));
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        return ENCODER.encodeToString(bytes) + "." + ENCODER.encodeToString(sign(bytes));
    }

    @Override public MembershipChangePreviewClaims verify(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 2) throw new InvalidMembershipPreview();
            byte[] payload = DECODER.decode(parts[0]);
            byte[] suppliedSignature = DECODER.decode(parts[1]);
            if (!MessageDigest.isEqual(sign(payload), suppliedSignature)) throw new InvalidMembershipPreview();
            String[] fields = new String(payload, StandardCharsets.UTF_8).split("\\|", -1);
            if (fields.length != 12 || !"v1".equals(decodeField(fields[0]))) throw new InvalidMembershipPreview();
            Instant expiresAt = Instant.ofEpochSecond(Long.parseLong(decodeField(fields[11])));
            if (!Instant.now().isBefore(expiresAt)) throw new InvalidMembershipPreview();
            String permissions = decodeField(fields[7]);
            Set<String> permissionSet = permissions.isEmpty() ? Set.of() : Set.copyOf(Arrays.asList(permissions.split(",")));
            String role = decodeField(fields[6]);
            return new MembershipChangePreviewClaims(UUID.fromString(decodeField(fields[1])),
                    decodeField(fields[2]), UUID.fromString(decodeField(fields[3])), decodeField(fields[4]),
                    MembershipChangeAction.valueOf(decodeField(fields[5])), role.isEmpty() ? null : MembershipRoleBundle.valueOf(role),
                    permissionSet, Long.parseLong(decodeField(fields[8])), Long.parseLong(decodeField(fields[9])),
                    decodeField(fields[10]), expiresAt);
        } catch (InvalidMembershipPreview invalid) {
            throw invalid;
        } catch (RuntimeException invalid) {
            throw new InvalidMembershipPreview();
        }
    }

    private byte[] sign(byte[] payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(payload);
        } catch (java.security.GeneralSecurityException impossible) {
            throw new IllegalStateException("Preview signing is unavailable", impossible);
        }
    }
    private static String encodeField(String value) { return ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8)); }
    private static String decodeField(String value) { return new String(DECODER.decode(value), StandardCharsets.UTF_8); }
}
