package com.olarize.identity.infrastructure.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record CurrentUserResponseDto(
        @Schema(description = "Keycloak ID of the person currently signed in.") String currentUserId) { }
