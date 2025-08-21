package eu.dataspace.connector.extension.kafka.broker.auth;

import com.fasterxml.jackson.annotation.JsonAlias;

public record OpenIdConfiguration(
        @JsonAlias("registration_endpoint")
        String registrationEndpoint,
        @JsonAlias("token_endpoint")
        String tokenEndpoint,
        @JsonAlias("userinfo_endpoint")
        String userInfoEndpoint
) {
}
