package com.harish.job_tracker.config;

import java.util.List;

import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationResolver;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;

public class AwsSecretsConfigDataLocationResolver
        implements ConfigDataLocationResolver<AwsSecretsResource> {

    private static final String PREFIX = "aws-secrets:";

    @Override
    public boolean isResolvable(
            ConfigDataLocationResolverContext context,
            ConfigDataLocation location) {

        return location.hasPrefix(PREFIX);
    }

    @Override
    public List<AwsSecretsResource> resolve(
            ConfigDataLocationResolverContext context,
            ConfigDataLocation location) {

        String secretName = location.getNonPrefixedValue(PREFIX);

        return List.of(new AwsSecretsResource(secretName));
    }
}