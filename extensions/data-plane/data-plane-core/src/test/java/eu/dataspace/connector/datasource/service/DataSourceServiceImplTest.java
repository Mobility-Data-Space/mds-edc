package eu.dataspace.connector.datasource.service;

import eu.dataspace.connector.datasource.spi.service.DataSourceService;
import eu.dataspace.connector.datasource.spi.store.DataSourceStore;
import eu.dataspace.connector.datasource.spi.types.DataSource;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataSourceServiceImplTest {

    private final DataSourceStore store = mock();
    private final TransactionContext transactionContext = new NoopTransactionContext();
    private final DataSourceService service = new DataSourceServiceImpl(store, transactionContext);

    @Test
    void create_generatesId_whenAbsent() {
        when(store.create(any())).thenAnswer(i -> StoreResult.success(i.getArgument(0)));

        var result = service.create(dataSource(null));

        assertThat(result).isSucceeded();
        assertThat(result.getContent().getId()).isNotBlank();
        verify(store).create(argThat(it -> it.getId() != null));
    }

    @Test
    void create_keepsProvidedId() {
        when(store.create(any())).thenAnswer(i -> StoreResult.success(i.getArgument(0)));

        var result = service.create(dataSource("provided-id"));

        assertThat(result).isSucceeded();
        assertThat(result.getContent().getId()).isEqualTo("provided-id");
    }

    @Test
    void update_mapsNotFound() {
        when(store.update(any())).thenReturn(StoreResult.notFound("nope"));

        assertThat(service.update(dataSource("id"))).isFailed();
    }

    @Test
    void findById() {
        when(store.findById("id")).thenReturn(StoreResult.success(dataSource("id")));

        assertThat(service.findById("id")).isSucceeded();
    }

    @Test
    void query() {
        when(store.query(any())).thenReturn(Stream.of(dataSource("a"), dataSource("b")));

        var result = service.query(QuerySpec.max());

        assertThat(result).isSucceeded();
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void deleteById_mapsNotFound() {
        when(store.deleteById("id")).thenReturn(StoreResult.notFound("nope"));

        assertThat(service.deleteById("id")).isFailed();
    }

    private DataSource dataSource(String id) {
        return DataSource.Builder.newInstance().id(id).name("a name").type("HttpData").build();
    }
}
