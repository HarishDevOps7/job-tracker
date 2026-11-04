package com.harish.job_tracker.config;
import java.io.IOException;
import java.util.Map;
import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoader;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.core.env.MapPropertySource;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;
public class AwsSecretsConfigDataLoader
        implements ConfigDataLoader<AwsSecretsResource> {
    private final SecretsManagerClient secretsManagerClient;
    public AwsSecretsConfigDataLoader() {
        this(SecretsManagerClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .build());
    }
    AwsSecretsConfigDataLoader(SecretsManagerClient client) {
        this.secretsManagerClient = client;
    }
    @Override
    public ConfigData load(
            ConfigDataLoaderContext context,
            AwsSecretsResource resource) throws IOException {
        try {
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(resource.getSecretName())
                    .build();
            String secretString =
                    secretsManagerClient.getSecretValue(request).secretString();
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> secrets = objectMapper.readValue(
                    secretString,
                    new TypeReference<Map<String, Object>>() {}
            );
            MapPropertySource propertySource =
                    new MapPropertySource("awsSecrets", secrets);
            return new ConfigData(java.util.List.of(propertySource));
        } catch (SecretsManagerException exception) {
            throw new IOException(
                    "Unable to retrieve Job Tracker secrets from AWS",
                    exception
            );
        }
    }
}
