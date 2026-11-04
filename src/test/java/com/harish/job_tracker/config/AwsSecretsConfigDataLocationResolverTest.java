package com.harish.job_tracker.config;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.config.ConfigDataLocation;
class AwsSecretsConfigDataLocationResolverTest {
    private final AwsSecretsConfigDataLocationResolver resolver =
            new AwsSecretsConfigDataLocationResolver();
    @Test
    void shouldRecognizeAwsSecretsLocation() {
        ConfigDataLocation location =
                ConfigDataLocation.of("aws-secrets:jobtracker/uat");
        assertTrue(resolver.isResolvable(null, location));
    }
    @Test
    void shouldRejectUnsupportedLocation() {
        ConfigDataLocation location =
                ConfigDataLocation.of("file:application.properties");
        assertFalse(resolver.isResolvable(null, location));
    }
    @Test
    void shouldResolveSecretName() {
        ConfigDataLocation location =
                ConfigDataLocation.of("aws-secrets:jobtracker/uat");
        var resources = resolver.resolve(null, location);
        assertEquals(1, resources.size());
        assertEquals("jobtracker/uat", resources.get(0).getSecretName());
    }
}
