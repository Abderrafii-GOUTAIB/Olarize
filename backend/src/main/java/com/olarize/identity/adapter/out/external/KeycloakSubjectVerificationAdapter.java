package com.olarize.identity.adapter.out.external;

import com.olarize.identity.application.port.out.IdentitySubjectVerificationPort;
import com.olarize.identity.domain.exception.IdentityProviderUnavailable;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
class KeycloakSubjectVerificationAdapter implements IdentitySubjectVerificationPort {
    private final RestClient client;
    private final String issuer;
    private final String tokenUrl;
    private final String adminBaseUrl;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    KeycloakSubjectVerificationAdapter(@Value("${olarize.auth.issuer}") String issuer,
            @Value("${olarize.identity-provider.token-uri:}") String tokenUrl,
            @Value("${olarize.identity-provider.admin-base-url:}") String adminBaseUrl,
            @Value("${olarize.identity-provider.realm:olarize}") String realm,
            @Value("${olarize.identity-provider.client-id:}") String clientId,
            @Value("${olarize.identity-provider.client-secret:}") String clientSecret) {
        this.client = RestClient.builder().build();
        this.issuer = issuer;
        this.tokenUrl = tokenUrl;
        this.adminBaseUrl = adminBaseUrl;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override public boolean exists(String subject) {
        if (adminBaseUrl.isBlank() || clientId.isBlank() || clientSecret.isBlank()) throw new IdentityProviderUnavailable();
        try {
            var form = new LinkedMultiValueMap<String, String>();
            form.add("grant_type", "client_credentials");
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);
            var token = client.post()
                    .uri(tokenUrl.isBlank() ? issuer + "/protocol/openid-connect/token" : tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(AccessToken.class);
            if (token == null || token.accessToken() == null || token.accessToken().isBlank()) {
                throw new IdentityProviderUnavailable();
            }
            URI userUri = UriComponentsBuilder.fromUriString(adminBaseUrl)
                    .pathSegment("admin", "realms", realm, "users", subject).build().encode().toUri();
            client.get().uri(userUri).headers(headers -> headers.setBearerAuth(token.accessToken()))
                    .retrieve().toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound missing) {
            return false;
        } catch (IdentityProviderUnavailable unavailable) {
            throw unavailable;
        } catch (Exception unavailable) {
            throw new IdentityProviderUnavailable();
        }
    }

    private record AccessToken(String access_token) {
        String accessToken() { return access_token; }
    }
}
