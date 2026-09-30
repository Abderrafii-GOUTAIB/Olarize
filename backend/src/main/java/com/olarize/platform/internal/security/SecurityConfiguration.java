package com.olarize.platform.internal.security;

import com.olarize.platform.internal.web.ApiProblems;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import tools.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class SecurityConfiguration {
    @Bean
    @Profile("local & !dev & !prod")
    @Order(1)
    SecurityFilterChain localDocumentation(HttpSecurity http) throws Exception {
        return http
            .securityMatcher("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/v3/api-docs", "/v3/api-docs/**",
                    "/swagger-ui.html", "/swagger-ui/**").permitAll()
                .anyRequest().denyAll())
            .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper, JwtDecoder decoder) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/identity", "/api/v1/identity/memberships", "/api/v1/identity/company-context").hasAuthority("SCOPE_profile.read")
                .requestMatchers(HttpMethod.GET, "/api/v1/companies/*/memberships").hasAuthority("SCOPE_profile.read")
                .requestMatchers(HttpMethod.GET, "/api/v1/companies/*/contracts/expiring").hasAuthority("SCOPE_profile.read")
                .requestMatchers(HttpMethod.POST, "/api/v1/companies/*/membership-change-previews",
                        "/api/v1/companies/*/membership-changes/confirm",
                        "/api/v1/companies/*/employees",
                        "/api/v1/companies/*/employees/*/episodes/*/contracts",
                        "/api/v1/companies/*/employees/*/episodes/*/contracts/*/corrections")
                    .hasAuthority("SCOPE_profile.read")
                .anyRequest().denyAll())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt.decoder(decoder))
                .authenticationEntryPoint((request, response, exception) -> writeProblem(response, mapper, HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler((request, response, exception) -> writeProblem(response, mapper, HttpStatus.FORBIDDEN)))
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .requestCache(AbstractHttpConfigurer::disable)
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) -> writeProblem(response, mapper, HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler((request, response, exception) -> writeProblem(response, mapper, HttpStatus.FORBIDDEN)))
            .build();
    }

    private static void writeProblem(HttpServletResponse response, ObjectMapper mapper, HttpStatus status)
            throws IOException {
        response.setStatus(status.value());
        if (status == HttpStatus.UNAUTHORIZED) response.setHeader("WWW-Authenticate", "Bearer");
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), ApiProblems.create(status));
    }

    @Bean
    UserDetailsService noLocalUsers() {
        return username -> { throw new UsernameNotFoundException("Local password authentication is unavailable"); };
    }
}
