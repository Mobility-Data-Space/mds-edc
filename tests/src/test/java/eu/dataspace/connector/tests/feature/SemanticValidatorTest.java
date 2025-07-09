package eu.dataspace.connector.tests.feature;

import eu.dataspace.connector.tests.MdsParticipant;
import eu.dataspace.connector.tests.MdsParticipantFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class SemanticValidatorTest {

    @RegisterExtension
    private static final MdsParticipant CONNECTOR = MdsParticipantFactory.inMemory("connector");

    @Test
    void shouldPassSemanticValidation() {
        var requestBody = """
                    {
                      "@context": {
                        "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
                        "dcat": "http://www.w3.org/ns/dcat#",
                        "dct": "http://purl.org/dc/terms/",
                        "owl": "http://www.w3.org/2002/07/owl#",
                        "mobilitydcatap": "https://w3id.org/mobilitydcat-ap/",
                        "mobilitydcatap-theme": "https://w3id.org/mobilitydcat-ap/mobility-theme/",\s
                        "adms": "http://www.w3.org/ns/adms#",
                        "edc": "https://w3id.org/edc/v0.0.1/ns/",
                        "skos": "http://www.w3.org/2004/02/skos/core#",
                        "rdf": "http://www.w3.org/2000/01/rdf-schema#"
                      },
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
                          "mobilitydcatap-theme:data-content-category": "INFRASTRUCTURE_AND_LOGISTICS",
                          "mobilitydcatap-theme:data-content-sub-category": "GENERAL_INFORMATION_ABOUT_PLANNING_OF_ROUTES"
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
                        "mobilitydcatap:transportMode": "ROAD",
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

}
