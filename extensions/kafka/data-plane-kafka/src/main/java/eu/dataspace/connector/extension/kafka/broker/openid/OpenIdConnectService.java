package eu.dataspace.connector.extension.kafka.broker.openid;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;


public class OpenIdConnectService {

    private final EdcHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenIdConnectService(final EdcHttpClient httpClient, final ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public ServiceResult<ClientRegistrationResponse> registerNewClient(OpenIdConfiguration openIdConfiguration, String accessToken) {
        var requestBody = Json.createObjectBuilder()
                .add("grant_types", Json.createArrayBuilder().add("client_credentials"))
                .build();

        var body = RequestBody.create(requestBody.toString(), MediaType.parse("application/json"));

        var request = new Request.Builder()
                .url(openIdConfiguration.registrationEndpoint())
                .post(body)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .build();

        return httpClient.execute(request, response -> handleResponse("registerNewClient", response, ClientRegistrationResponse.class))
                .flatMap(ServiceResult::from);
    }

    public ServiceResult<OpenIdConfiguration> fetchOpenIdConfiguration(String discoveryUrl) {
        var request = new Request.Builder()
                .url(discoveryUrl)
                .get()
                .build();

        return httpClient.execute(request, response -> handleResponse("fetchOpenIdConfiguration", response, OpenIdConfiguration.class))
                .flatMap(ServiceResult::from);
    }

    public ServiceResult<UserInfo> userInfo(OpenIdConfiguration openIdConfiguration, ClientRegistrationResponse clientRegistrationResponse) {
        return issueToken(openIdConfiguration, clientRegistrationResponse)
                .compose(token -> {
                    var userInfoRequest = new Request.Builder().url(openIdConfiguration.userInfoEndpoint()).addHeader("Authorization", token.tokenType() + " " + token.accessToken()).build();
                    return httpClient.execute(userInfoRequest, response -> handleResponse("userinfo", response, UserInfo.class))
                            .flatMap(ServiceResult::from);
                });
    }

    private ServiceResult<TokenResponse> issueToken(OpenIdConfiguration openIdConfiguration, ClientRegistrationResponse clientRegistrationResponse) {
        var tokenRequestBody = new FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("client_id", clientRegistrationResponse.clientId())
                .add("client_secret", clientRegistrationResponse.clientSecret())
                .add("scope", "openid roles")
                .build();
        var tokenRequest = new Request.Builder().url(openIdConfiguration.tokenEndpoint()).post(tokenRequestBody).build();

        return httpClient.execute(tokenRequest, response -> handleResponse("token", response, TokenResponse.class))
                .flatMap(ServiceResult::from);
    }

    private @NotNull <T> Result<T> handleResponse(String callName, Response response, Class<T> type) {
        if (!response.isSuccessful()) {
            return Result.failure(callName + " responded with " + response.code());
        }

        try (var responseBody = response.body()) {
            return Result.success(objectMapper.readValue(responseBody.byteStream(), type));
        } catch (IOException e) {
            return Result.failure("Cannot deserialize response: " + e.getMessage());
        }
    }

}
