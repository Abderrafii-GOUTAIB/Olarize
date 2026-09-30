package com.olarize.assistant.infrastructure.out.provider;

import com.olarize.assistant.application.exception.AssistantUnavailable;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Configuration(proxyBeanMethods = false)
class ExpiryProviderConfiguration {
    @Bean Clock assistantClock() { return Clock.system(ZoneId.of("Africa/Casablanca")); }
    @Bean(destroyMethod = "close") BoundedExpiryIntentAdapter expiryIntentProvider(Environment environment) {
        boolean synthetic = environment.acceptsProfiles(Profiles.of("local & !dev & !prod"));
        var planner = new SyntheticExpiryPlanner();
        return new BoundedExpiryIntentAdapter(question -> {
            if (!synthetic) throw new AssistantUnavailable();
            return planner.plan(question);
        }, Duration.ofSeconds(2));
    }
}
