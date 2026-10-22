package eu.dataspace.connector.datasource.store;

import eu.dataspace.connector.datasource.spi.store.DataSourceStore;
import eu.dataspace.connector.datasource.spi.types.DataSource;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

public abstract class DataSourceStoreTestBase {

    @Test
    void create_andFindById() {
        var dataSource = dataSource("ds-1", "a name", "HttpData", Map.of("baseUrl", "http://localhost"));

        var created = getStore().create(dataSource);

        assertThat(created).isSucceeded();
        var found = getStore().findById("ds-1");
        assertThat(found).isSucceeded();
        assertThat(found.getContent().getName()).isEqualTo("a name");
        assertThat(found.getContent().getType()).isEqualTo("HttpData");
        assertThat(found.getContent().getProperties()).containsEntry("baseUrl", "http://localhost");
        assertThat(found.getContent().getCreatedAt()).isGreaterThan(0);
        assertThat(found.getContent().getUpdatedAt()).isGreaterThan(0);
    }

    @Test
    void create_whenAlreadyExists() {
        var dataSource = dataSource("ds-1", "a name", "HttpData", Map.of());
        getStore().create(dataSource);

        var result = getStore().create(dataSource("ds-1", "another", "Kafka", Map.of()));

        assertThat(result).isFailed()
                .detail().isEqualTo(DataSourceStore.ALREADY_EXISTS_TEMPLATE.formatted("ds-1"));
    }

    @Test
    void findById_whenNotExists() {
        assertThat(getStore().findById("missing")).isFailed()
                .detail().isEqualTo(DataSourceStore.NOT_FOUND_TEMPLATE.formatted("missing"));
    }

    @Test
    void update() {
        getStore().create(dataSource("ds-1", "a name", "HttpData", Map.of("baseUrl", "http://old")));
        var created = getStore().findById("ds-1").getContent();

        var result = getStore().update(dataSource("ds-1", "new name", "Kafka", Map.of("topic", "events")));

        assertThat(result).isSucceeded();
        var updated = getStore().findById("ds-1").getContent();
        assertThat(updated.getName()).isEqualTo("new name");
        assertThat(updated.getType()).isEqualTo("Kafka");
        assertThat(updated.getProperties()).containsEntry("topic", "events").doesNotContainKey("baseUrl");
        assertThat(updated.getCreatedAt()).isEqualTo(created.getCreatedAt());
        assertThat(updated.getUpdatedAt()).isGreaterThanOrEqualTo(created.getUpdatedAt());
    }

    @Test
    void update_whenNotExists() {
        var result = getStore().update(dataSource("missing", "a name", "HttpData", Map.of()));

        assertThat(result).isFailed()
                .detail().isEqualTo(DataSourceStore.NOT_FOUND_TEMPLATE.formatted("missing"));
    }

    @Test
    void query_filtersByNameAndType() {
        getStore().create(dataSource(UUID.randomUUID().toString(), "http-one", "HttpData", Map.of()));
        getStore().create(dataSource(UUID.randomUUID().toString(), "http-two", "HttpData", Map.of()));
        getStore().create(dataSource(UUID.randomUUID().toString(), "kafka-one", "Kafka", Map.of()));

        var byType = getStore().query(queryBy("type", "HttpData")).collect(Collectors.toList());
        assertThat(byType).hasSize(2).allSatisfy(it -> assertThat(it.getType()).isEqualTo("HttpData"));

        var byName = getStore().query(queryBy("name", "kafka-one")).collect(Collectors.toList());
        assertThat(byName).hasSize(1).first().extracting(DataSource::getName).isEqualTo("kafka-one");
    }

    @Test
    void query_all() {
        getStore().create(dataSource(UUID.randomUUID().toString(), "one", "HttpData", Map.of("k", "v")));
        getStore().create(dataSource(UUID.randomUUID().toString(), "two", "Kafka", Map.of()));

        var all = getStore().query(QuerySpec.max()).collect(Collectors.toList());

        assertThat(all).hasSize(2);
    }

    @Test
    void deleteById() {
        getStore().create(dataSource("ds-1", "a name", "HttpData", Map.of()));

        assertThat(getStore().deleteById("ds-1")).isSucceeded();
        assertThat(getStore().findById("ds-1")).isFailed();
    }

    @Test
    void deleteById_whenNotExists() {
        assertThat(getStore().deleteById("missing")).isFailed()
                .detail().isEqualTo(DataSourceStore.NOT_FOUND_TEMPLATE.formatted("missing"));
    }

    protected abstract DataSourceStore getStore();

    private DataSource dataSource(String id, String name, String type, Map<String, Object> properties) {
        return DataSource.Builder.newInstance()
                .id(id)
                .name(name)
                .type(type)
                .properties(properties)
                .build();
    }

    private QuerySpec queryBy(String field, String value) {
        return QuerySpec.Builder.newInstance()
                .filter(Criterion.Builder.newInstance()
                        .operandLeft(field)
                        .operator("=")
                        .operandRight(value)
                        .build())
                .build();
    }
}
