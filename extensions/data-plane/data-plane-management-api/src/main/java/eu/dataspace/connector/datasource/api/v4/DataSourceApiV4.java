package eu.dataspace.connector.datasource.api.v4;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.api.management.schema.ManagementApiJsonSchema;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;

@Tag(name = "Data Source v4", description = "Manage Data Sources: named, reusable sets of properties describing a " +
        "backend, used as a creation-time template when authoring assets. Editing or deleting a data source has no " +
        "effect on existing assets.")
public interface DataSourceApiV4 {

    @Operation(description = "Creates a new data source.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = DataSourceSchema.class))),
            responses = {
                    @ApiResponse(responseCode = "201", description = "The data source was created",
                            content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.ID_RESPONSE))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR))))
            })
    Response createDataSourceV4(JsonObject dataSource);

    @Operation(description = "Updates an existing data source.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = DataSourceSchema.class))),
            responses = {
                    @ApiResponse(responseCode = "204", description = "The data source was updated"),
                    @ApiResponse(responseCode = "404", description = "No data source with the given id exists"),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR))))
            })
    void updateDataSourceV4(JsonObject dataSource);

    @Operation(description = "Query the data sources, filtering over name and type only.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(ref = ManagementApiJsonSchema.V4.QUERY_SPEC))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "A list of the matching data sources"),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(ref = ManagementApiJsonSchema.V4.API_ERROR))))
            })
    JsonArray requestDataSourcesV4(JsonObject querySpecJson);

    @Operation(description = "Gets a data source by id.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The data source"),
                    @ApiResponse(responseCode = "404", description = "No data source with the given id exists")
            })
    JsonObject getDataSourceV4(@Parameter(name = "id", description = "The data source id") String id);

    @Operation(description = "Deletes a data source by id.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "The data source was deleted"),
                    @ApiResponse(responseCode = "404", description = "No data source with the given id exists")
            })
    void deleteDataSourceV4(@Parameter(name = "id", description = "The data source id") String id);

    @Schema(name = "DataSourceExample", example = DataSourceSchema.EXAMPLE)
    record DataSourceSchema(
            @Schema(name = ID) String id,
            String name,
            String type
    ) {
        public static final String EXAMPLE = """
                {
                    "@context": "https://w3id.org/mobility-dataspace/connector/management/v1",
                    "@type": "DataSource",
                    "@id": "data-source-id",
                    "name": "My HTTP backend",
                    "type": "HttpData",
                    "properties": {
                        "baseUrl": "https://backend.example.com/data",
                        "authKey": "Authorization",
                        "secretName": "backend-credential-alias"
                    }
                }
                """;
    }
}
