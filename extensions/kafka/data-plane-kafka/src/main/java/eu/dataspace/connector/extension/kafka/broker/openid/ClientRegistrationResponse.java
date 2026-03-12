package eu.dataspace.connector.extension.kafka.broker.openid;

import com.fasterxml.jackson.annotation.JsonAlias;

public record ClientRegistrationResponse(
        @JsonAlias("client_id")
        String clientId,
        @JsonAlias("client_secret")
        String clientSecret,
        @JsonAlias("registration_client_uri")
        String registrationClientUri,
        @JsonAlias("registration_access_token")
        String registrationAccessToken
) {
}
