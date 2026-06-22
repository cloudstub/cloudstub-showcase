package io.cloudstub.showcase.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads the database credentials from Secrets Manager. The secret value is a JSON document with
 * {@code username} and {@code password} fields, the format the AWS console produces for an RDS
 * credentials secret.
 */
@Component
public class DatabaseCredentialsProvider {

    private final SecretsManagerClient client;
    private final String secretId;
    private final ObjectMapper mapper;

    public DatabaseCredentialsProvider(
            SecretsManagerClient client,
            ObjectMapper mapper,
            @Value("${showcase.db-secret-name}") String secretId) {
        this.client = client;
        this.mapper = mapper;
        this.secretId = secretId;
    }

    public DbCredentials fetch() {
        String secret = client.getSecretValue(b -> b.secretId(secretId)).secretString();
        try {
            JsonNode node = mapper.readTree(secret);
            return new DbCredentials(node.get("username").asText(), node.get("password").asText());
        } catch (Exception e) {
            throw new IllegalStateException("Could not parse database credentials secret", e);
        }
    }
}
