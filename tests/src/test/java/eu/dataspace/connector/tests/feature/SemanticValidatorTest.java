package eu.dataspace.connector.tests.feature;

import eu.dataspace.connector.tests.MdsParticipant;
import eu.dataspace.connector.tests.MdsParticipantFactory;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;

public class SemanticValidatorTest {

    private static final Path VOCABULARY_DOCUMENTATION = Path.of("../docs/usage/mds_features/mds_vocabulary_semantics.md");
    private static final Pattern JSON_BLOCK = Pattern.compile("```json\\n(.*?)```", Pattern.DOTALL);
    private static final String MANAGEMENT_CONTEXT = "https://w3id.org/mobility-dataspace/connector/management/v1";

    @RegisterExtension
    private static final MdsParticipant CONNECTOR = MdsParticipantFactory.inMemory("connector");

    @Test
    void shouldPassSemanticValidation() {
        var requestBody = """
                    {
                      "@context": "https://w3id.org/mobility-dataspace/connector/management/v1",
                      "@type": "Asset",
                      "properties": {
                        "dct:title": "My Asset",
                        "dct:description": "Lorem Ipsum ...",
                        "dct:language": "code/EN",
                        "dct:publisher": "https://data-source.my-org/about",
                        "dct:license": "https://data-source.my-org/license",
                        "dct:rightsHolder": "my-sovereign-legal-name",
                        "dct:accessRights": "usage policies and rights",
                        "dct:spatial": {
                          "skos:prefLabel": "my-geo-location",
                          "dct:identifier": ["DE", "DE636"]
                        },
                        "dct:isReferencedBy": "https://data-source.my-org/references",
                        "dct:temporal": {
                          "dcat:startDate": "2024-02-01",
                          "dcat:endDate": "2024-02-10"
                        },
                        "dct:accrualPeriodicity": "every month",
                    
                        "dcat:organization": "Company Name",
                        "dcat:keywords": ["some", "keywords"],
                        "dcat:mediaType": "application/json",
                        "dcat:landingPage": "https://data-source.my-org/docs",
                    
                        "owl:versionInfo": "1.1",
                    
                        "mobilitydcatap:mobilityTheme": {
                          "mobilitydcatap-theme:data-content-category": "FILLING_AND_CHARGING_STATIONS",
                          "mobilitydcatap-theme:data-content-sub-category": "AVAILABILITY_OF_CHARGING_POINTS_FOR_ELECTRIC_VEHICLES"
                        },
                        "mobilitydcatap:mobilityDataStandard": {
                          "@id": "my-data-model-001",
                          "mobilitydcatap:schema": {
                            "dcat:downloadURL": [
                              "https://teamabc.departmentxyz.schema/a",
                              "https://teamabc.departmentxyz.schema/b"
                            ],
                            "rdf:Literal": "These reference files are important"
                          }
                        },
                        "mobilitydcatap:transportMode": "BICYCLE",
                        "mobilitydcatap:georeferencingMethod": "my-geo-reference-method",
                    
                        "adms:sample": ["https://teamabc.departmentxyz.sample/a", "https://teamabc.departmentxyz.sample/b"],
                    
                        "additionalProperties": {}
                      },
                      "privateProperties": {
                        "privateKey": "privateValue"
                      },
                      "dataAddress": {
                        "type": "HttpData",
                          "name": "Example",
                          "baseUrl": "https://example.com/"
                      }
                    }
                    """;

        var id = CONNECTOR.createAsset(requestBody);

        assertThat(id).isNotNull();
    }

    @Test
    void shouldPassSemanticValidation_whenMobilityThemeIsSplitInMultipleNodes() {
        var requestBody = """
                    {
                      "@context": "https://w3id.org/mobility-dataspace/connector/management/v1",
                      "@type": "Asset",
                      "properties": {
                        "dct:title": "My Asset",
                        "mobilitydcatap:mobilityTheme": [
                          { "mobilitydcatap-theme:data-content-category": "FILLING_AND_CHARGING_STATIONS" },
                          { "mobilitydcatap-theme:data-content-sub-category": "AVAILABILITY_OF_FILLING_STATIONS" }
                        ]
                      },
                      "dataAddress": {
                        "type": "HttpData",
                        "baseUrl": "https://example.com/"
                      }
                    }
                    """;

        var id = CONNECTOR.createAsset(requestBody);

        assertThat(id).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("documentationExamples")
    void shouldAcceptDocumentationExamples(JsonObject example) {
        var requestBody = example.containsKey("@type") ? example : assetWithProperties(example);

        var id = CONNECTOR.createAsset(requestBody.toString());

        assertThat(id).isNotNull();
    }

    /**
     * JSON examples of the vocabulary documentation: complete assets are submitted as they are, property fragments are
     * merged into a minimal asset. The block showing the bare context is skipped.
     */
    private static Stream<JsonObject> documentationExamples() throws IOException {
        var examples = JSON_BLOCK.matcher(Files.readString(VOCABULARY_DOCUMENTATION)).results()
                .map(match -> Json.createReader(new StringReader(match.group(1))).readObject())
                .filter(example -> !example.keySet().equals(Set.of("@context")))
                .toList();
        assertThat(examples).as("JSON examples in %s", VOCABULARY_DOCUMENTATION).isNotEmpty();
        return examples.stream();
    }

    private static JsonObject assetWithProperties(JsonObject properties) {
        return createObjectBuilder()
                .add("@context", MANAGEMENT_CONTEXT)
                .add("@type", "Asset")
                .add("properties", createObjectBuilder()
                        .add("dct:title", "My Asset")
                        .add("mobilitydcatap:mobilityTheme", createObjectBuilder()
                                .add("mobilitydcatap-theme:data-content-category", "OTHER"))
                        .addAll(createObjectBuilder(properties)))
                .add("dataAddress", createObjectBuilder()
                        .add("type", "HttpData")
                        .add("baseUrl", "https://example.com/"))
                .build();
    }

}
