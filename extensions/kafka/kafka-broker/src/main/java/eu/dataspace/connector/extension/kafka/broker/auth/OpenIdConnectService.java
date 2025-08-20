package eu.dataspace.connector.extension.kafka.broker.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
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

    public ServiceResult<ClientRegistrationResponse> registerNewClient(String openidConnectDiscoveryUrl, String accessToken) {
        return fetchOpenIdConfiguration(openidConnectDiscoveryUrl)
                .compose(openIdConfiguration -> {
                    var requestBody = Json.createObjectBuilder()
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
                });
    }

    private ServiceResult<OpenIdConfiguration> fetchOpenIdConfiguration(String discoveryUrl) {
        var request = new Request.Builder()
                .url(discoveryUrl)
                .get()
                .build();

        return httpClient.execute(request, response -> handleResponse("fetchOpenIdConfiguration", response, OpenIdConfiguration.class))
                .flatMap(ServiceResult::from);
    }

    private @NotNull <T> Result<T> handleResponse(String callName, Response response, Class<T> type) {
        if (!response.isSuccessful()) {
            return Result.failure(callName + " responded with " + response.code());
        }

        var responseBody = response.body();
        if (responseBody == null) {
            return Result.failure("Response body is null");
        }

        try (responseBody) {
            return Result.success(objectMapper.readValue(responseBody.byteStream(), type));
        } catch (IOException e) {
            return Result.failure("Cannot deserialize response: " + e.getMessage());
        }
    }

    record OpenIdConfiguration(
            @JsonAlias("registration_endpoint")
            String registrationEndpoint
    ) {}
}
