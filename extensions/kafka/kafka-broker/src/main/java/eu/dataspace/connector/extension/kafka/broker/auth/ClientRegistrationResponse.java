package eu.dataspace.connector.extension.kafka.broker.auth;

import com.fasterxml.jackson.annotation.JsonAlias;

public record ClientRegistrationResponse(
        @JsonAlias("client_id")
        String clientId,
        @JsonAlias("client_secret")
        String clientSecret
) {
}
