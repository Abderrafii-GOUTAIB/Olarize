package com.olarize.identity.adapter.in.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CurrentUserController {
    @GetMapping(value = "/api/v1/identity", produces = MediaType.APPLICATION_JSON_VALUE)
    CurrentUserResponse currentUser(@AuthenticationPrincipal Jwt principal) {
        return new CurrentUserResponse(principal.getSubject());
    }

    record CurrentUserResponse(@Schema(description = "Keycloak ID of the person currently signed in.")
            String currentUserId) { }
}
