package eu.dataspace.connector.dataplane.kafka.spi;

public record Credentials(
        String subject,
        String tokenEndpoint,
        String clientId,
        String clientSecret
) {
}
