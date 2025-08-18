package eu.dataspace.connector.extension.kafka.broker.auth;

/**
 * Interface for services that handle OAuth2 access token operations for Kafka authentication.
 * Defines methods to fetch and revoke OAuth2 access tokens using the Client Credentials flow.
 */
public interface KafkaOAuthService {
// TODO Will probably be part of the Data Plane Authorization Service?
    /**
     * Always performs a client_credentials flow and returns a fresh token.
     *
     * @param creds The OAuth credentials to use for token acquisition
     * @return The acquired access token as a string
     */
    String getAccessToken(OAuthCredentials creds);

    /**
     * Revokes the given token.
     *
     * @param creds The OAuth credentials used for token revocation
     * @param token The token to revoke
     */
    void revokeToken(OAuthCredentials creds, String token);

    // void getNewClient() ;
    // void deleteClient() ;
}