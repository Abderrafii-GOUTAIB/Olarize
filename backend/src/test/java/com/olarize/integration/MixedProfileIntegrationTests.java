package com.olarize.integration;

import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"prod", "local"})
class MixedProfileIntegrationTests extends SharedDeploymentProfileIntegrationTests { }
