package eu.dataspace.connector.extension.kafka.broker.auth;

import com.fasterxml.jackson.annotation.JsonAlias;

public record TokenResponse(
        @JsonAlias("access_token")
        String accessToken,
        @JsonAlias("token_type")
        String tokenType
) {
}
