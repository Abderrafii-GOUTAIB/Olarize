package com.olarize.identity.infrastructure.in.web.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.olarize.identity.infrastructure.in.web.dto.response.CurrentUserResponseDto;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CurrentUserController {
    @GetMapping(value = "/api/v1/identity", produces = MediaType.APPLICATION_JSON_VALUE)
    CurrentUserResponseDto currentUser(@AuthenticationPrincipal Jwt principal) {
        return new CurrentUserResponseDto(principal.getSubject());
    }

}
