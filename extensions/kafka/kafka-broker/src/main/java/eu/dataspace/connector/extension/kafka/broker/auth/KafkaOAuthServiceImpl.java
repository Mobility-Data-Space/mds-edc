package eu.dataspace.connector.extension.kafka.broker.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.edc.http.spi.EdcHttpClient;

import java.io.IOException;

/**
 * Stateless service to fetch and revoke OAuth2 access tokens using the Client Credentials flow.
 * No token is cached—each getAccessToken() call always retrieves a new token.
 */
public class KafkaOAuthServiceImpl implements KafkaOAuthService {
    static final String ACCESS_TOKEN_KEY = "access_token";
    static final String GRANT_TYPE_KEY = "grant_type";
    static final String CLIENT_CREDENTIALS_GRANT_TYPE = "client_credentials";
    static final String CLIENT_ID_KEY = "client_id";
    static final String CLIENT_SECRET_KEY = "client_secret";
    static final String CONTENT_TYPE_HEADER = "Content-Type";
    static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
    static final String TOKEN_KEY = "token";

    private final EdcHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public KafkaOAuthServiceImpl(final EdcHttpClient httpClient, final ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Always performs a client_credentials flow and returns a fresh token.
     */
    public String getAccessToken(final OAuthCredentials creds) {
        return fetchNewToken(creds);
    }

    private String fetchNewToken(final OAuthCredentials creds) {
        try {
            FormBody formBody = new FormBody.Builder()
                    .add(GRANT_TYPE_KEY, CLIENT_CREDENTIALS_GRANT_TYPE)
                    .add(CLIENT_ID_KEY, creds.clientId())
                    .add(CLIENT_SECRET_KEY, creds.clientSecret())
                    .build();

            Request request = new Request.Builder()
                    .url(creds.tokenUrl())
                    .header(CONTENT_TYPE_HEADER, APPLICATION_X_WWW_FORM_URLENCODED)
                    .post(formBody)
                    .build();

            try (Response response = httpClient.execute(request)) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("OAuth2 token endpoint returned HTTP " + response.code());
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                JsonNode json = objectMapper.readTree(responseBody);
                return json.get(ACCESS_TOKEN_KEY).asText();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch OAuth2 token", e);
        }
    }

    /**
     * Revokes the given token.
     */
    public void revokeToken(final OAuthCredentials creds, final String token) {
        if (creds.revocationUrl().isEmpty()) {
            return;
        }
        try {
            FormBody formBody = new FormBody.Builder()
                    .add(TOKEN_KEY, token)
                    .add(CLIENT_ID_KEY, creds.clientId())
                    .add(CLIENT_SECRET_KEY, creds.clientSecret())
                    .build();

            Request request = new Request.Builder()
                    .url(creds.revocationUrl().get())
                    .header(CONTENT_TYPE_HEADER, APPLICATION_X_WWW_FORM_URLENCODED)
                    .post(formBody)
                    .build();

            try (Response response = httpClient.execute(request)) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Revoke endpoint returned HTTP " + response.code());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to revoke OAuth2 token", e);
        }
    }
}