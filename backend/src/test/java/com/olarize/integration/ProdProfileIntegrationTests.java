package com.olarize.integration;

import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("prod")
class ProdProfileIntegrationTests extends SharedDeploymentProfileIntegrationTests { }
