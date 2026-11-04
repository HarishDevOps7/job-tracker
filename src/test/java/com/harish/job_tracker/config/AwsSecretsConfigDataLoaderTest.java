package com.harish.job_tracker.config;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;
class AwsSecretsConfigDataLoaderTest {
    @Test
    void shouldLoadSecretsSuccessfully() throws IOException {
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(any(GetSecretValueRequest.class)))
                .thenReturn(GetSecretValueResponse.builder()
                        .secretString("{\"DB_USERNAME\":\"jobapp\"}")
                        .build());
        AwsSecretsConfigDataLoader loader =
                new AwsSecretsConfigDataLoader(client);
        var configData = loader.load(
                null, new AwsSecretsResource("jobtracker/uat"));
        assertNotNull(configData);
        assertEquals("jobapp",
                configData.getPropertySources().get(0)
                        .getProperty("DB_USERNAME"));
        verify(client).getSecretValue(any(GetSecretValueRequest.class));
    }
    @Test
    void shouldWrapAwsExceptionAsIOException() {
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(any(GetSecretValueRequest.class)))
                .thenThrow(SecretsManagerException.builder()
                        .message("Access denied")
                        .build());
        AwsSecretsConfigDataLoader loader =
                new AwsSecretsConfigDataLoader(client);
        assertThrows(IOException.class, () ->
                loader.load(null, new AwsSecretsResource("jobtracker/uat")));
    }
}
