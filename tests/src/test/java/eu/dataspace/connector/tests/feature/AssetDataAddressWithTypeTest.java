package eu.dataspace.connector.tests.feature;

import eu.dataspace.connector.tests.MdsParticipant;
import eu.dataspace.connector.tests.MdsParticipantFactory;
import jakarta.json.Json;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

/**
 * Reproduces MDS-Project#1114: creating an asset whose {@code dataAddress} contains {@code @type}
 * fails with HTTP 500 (ClassCastException in JsonObjectToDataplaneMetadataTransformer) instead of
 * being mapped to dataplaneMetadata with a deprecation warning.
 */
class AssetDataAddressWithTypeTest {

    @RegisterExtension
    private static final MdsParticipant CONNECTOR = MdsParticipantFactory.inMemory("connector");

    @Test
    void shouldCreateAsset_whenDataAddressContainsType() {
        var assetId = UUID.randomUUID().toString();
        var body = Json.createObjectBuilder()
                .add(CONTEXT, Json.createValue("https://w3id.org/mobility-dataspace/connector/management/v1"))
                .add(TYPE, "Asset")
                .add(ID, assetId)
                .add("properties", Json.createObjectBuilder()
                        .add("dct:title", "any")
                        .add("mobilitydcatap:mobilityTheme", Json.createObjectBuilder()
                                .add("mobilitydcatap-theme:data-content-category", "OTHER")))
                .add("dataAddress", Json.createObjectBuilder()
                        .add(TYPE, "DataAddress")
                        .add("type", "HttpData")
                        .add("baseUrl", "https://example.com"))
                .build();

        CONNECTOR.baseManagementRequest()
                .contentType(JSON)
                .body(body)
                .when().post("/assets")
                .then()
                .log().ifValidationFails()
                .statusCode(200);

        // the dataAddress is mapped to dataplaneMetadata.properties, @type stripped, values preserved
        CONNECTOR.baseManagementRequest()
                .contentType(JSON)
                .when().get("/assets/{id}", assetId)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("'dataplaneMetadata'.properties.'edc:type'", org.hamcrest.Matchers.equalTo("HttpData"))
                .body("'dataplaneMetadata'.properties.'edc:baseUrl'", org.hamcrest.Matchers.equalTo("https://example.com"));
    }
}
