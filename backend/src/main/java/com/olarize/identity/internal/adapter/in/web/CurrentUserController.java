package com.olarize.identity.internal.adapter.in.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CurrentUserController {
    @GetMapping(value = "/api/v1/me", produces = MediaType.APPLICATION_JSON_VALUE)
    CurrentUserResponse currentUser(@AuthenticationPrincipal Jwt principal) {
        return new CurrentUserResponse(principal.getSubject());
    }

    record CurrentUserResponse(String subject) { }
}
