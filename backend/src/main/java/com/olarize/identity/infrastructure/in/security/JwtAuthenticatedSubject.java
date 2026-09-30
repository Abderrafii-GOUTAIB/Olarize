package com.olarize.identity.infrastructure.in.security;

import com.olarize.identity.application.port.out.AuthenticatedSubjectPort;
import com.olarize.identity.domain.exception.CompanyAccessDenied;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
class JwtAuthenticatedSubject implements AuthenticatedSubjectPort {
    @Override public String require() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwt) || !jwt.isAuthenticated()
                || jwt.getToken().getSubject() == null || jwt.getToken().getSubject().isBlank()
                || jwt.getAuthorities().stream().noneMatch(a -> "SCOPE_profile.read".equals(a.getAuthority()))) {
            throw new CompanyAccessDenied();
        }
        return jwt.getToken().getSubject();
    }
}
