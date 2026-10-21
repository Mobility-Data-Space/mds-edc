package eu.dataspace.connector.dataplane.api.v4;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.management.schema.ManagementApiJsonSchema;

@OpenAPIDefinition(info = @Info(description = "With this API clients can query the data flows known to the (embedded) " +
        "data plane and retrieve the data address a flow is reading from. These endpoints replace the EDR cache " +
        "endpoints that were removed with Management API V3.", title = "DataFlow API", version = "v4"))
@Tag(name = "DataFlow v4")
public interface DataFlowApiV4 {

    @Operation(description = "Query the data flows known to the data plane.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.QUERY_SPEC))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "A list of the matching data flows"),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR))))
            })
    JsonArray requestDataFlowsV4(JsonObject querySpecJson);

    @Operation(description = "Retrieve the source data address of a data flow.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The source data address of the data flow"),
                    @ApiResponse(responseCode = "404", description = "No data flow with the given id exists")
            })
    JsonObject getDataAddressV4(@Parameter(name = "id", description = "The data flow id (matches the transfer process id)") String id);
}
