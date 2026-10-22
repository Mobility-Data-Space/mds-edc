package eu.dataspace.connector.tests.feature;

import eu.dataspace.connector.tests.MdsParticipant;
import eu.dataspace.connector.tests.MdsParticipantFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;

class DataSourceManagementApiTest {

    @RegisterExtension
    private static final MdsParticipant CONNECTOR = MdsParticipantFactory.inMemory("connector");

    @Test
    void shouldManageDataSourceLifecycle() {
        var id = CONNECTOR.createDataSource("my-backend", "HttpData", Map.of(
                "baseUrl", "https://backend.example.com/data",
                "secretName", "backend-credential-alias"
        ));

        // the created data source is retrievable, with its opaque properties hydrated from the vault
        var dataSource = CONNECTOR.getDataSource(id).statusCode(200).extract().body().jsonPath();
        assertThat(dataSource.getString(ID)).isEqualTo(id);
        assertThat(dataSource.getString("name")).isEqualTo("my-backend");
        assertThat(dataSource.getString("type")).isEqualTo("HttpData");
        assertThat(dataSource.getString("properties.baseUrl")).isEqualTo("https://backend.example.com/data");
        assertThat(dataSource.getString("properties.secretName")).isEqualTo("backend-credential-alias");

        // it can be found by querying (over name and type)
        assertThat(CONNECTOR.requestDataSources())
                .anySatisfy(it -> assertThat(it.asJsonObject().getString(ID)).isEqualTo(id));

        // editing replaces name, type and properties
        CONNECTOR.updateDataSource(id, "my-backend-renamed", "HttpData", Map.of("baseUrl", "https://new.example.com"))
                .statusCode(204);
        var updated = CONNECTOR.getDataSource(id).statusCode(200).extract().body().jsonPath();
        assertThat(updated.getString("name")).isEqualTo("my-backend-renamed");
        assertThat(updated.getString("properties.baseUrl")).isEqualTo("https://new.example.com");

        // deleting removes it
        CONNECTOR.deleteDataSource(id).statusCode(204);
        CONNECTOR.getDataSource(id).statusCode(404);
    }

    @Test
    void shouldReturnNotFound_whenDataSourceDoesNotExist() {
        CONNECTOR.getDataSource("unexistent").statusCode(404);
        CONNECTOR.deleteDataSource("unexistent").statusCode(404);
        CONNECTOR.updateDataSource("unexistent", "name", "HttpData", Map.of()).statusCode(404);
    }
}
