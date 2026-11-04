package com.harish.job_tracker.config;

import org.springframework.boot.context.config.ConfigDataResource;

public class AwsSecretsResource extends ConfigDataResource {

    private final String secretName;

    public AwsSecretsResource(String secretName) {
        this.secretName = secretName;
    }

    public String getSecretName() {
        return secretName;
    }
}