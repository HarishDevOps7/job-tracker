package com.harish.job_tracker.config;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
class AwsSecretsResourceTest {
    @Test
    void shouldStoreAndReturnSecretName() {
        AwsSecretsResource resource =
                new AwsSecretsResource("jobtracker/uat");
        assertEquals("jobtracker/uat", resource.getSecretName());
    }
}
