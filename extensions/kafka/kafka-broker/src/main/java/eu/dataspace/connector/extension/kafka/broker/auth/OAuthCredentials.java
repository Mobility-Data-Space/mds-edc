package eu.dataspace.connector.extension.kafka.broker.auth;

import java.util.Optional;

/**
 * Simple immutable holder for the four OAuth2 parameters.
 *
 * @param tokenUrl       The endpoint URL to fetch OAuth2 tokens using client credentials flow.
 * @param revocationUrl  The optional endpoint URL used to revoke tokens. This may be empty if token revocation is not supported.
 * @param clientId       The identifier of the client application attempting to authenticate.
 * @param clientSecret   The secret associated with the client identifier for authentication.
 */
public record OAuthCredentials(String tokenUrl, Optional<String> revocationUrl, String clientId, String clientSecret) {

}
