package eu.dataspace.connector.datasource.api;

import eu.dataspace.connector.datasource.spi.service.DataSourceService;
import eu.dataspace.connector.datasource.spi.types.DataSource;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;

import static eu.dataspace.connector.datasource.spi.types.DataSource.DATA_SOURCE_TYPE;
import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

public abstract class BaseDataSourceApiController {

    protected final DataSourceService service;
    protected final TypeTransformerRegistry transformerRegistry;
    protected final JsonObjectValidatorRegistry validator;
    protected final Monitor monitor;

    public BaseDataSourceApiController(DataSourceService service, TypeTransformerRegistry transformerRegistry,
                                       JsonObjectValidatorRegistry validator, Monitor monitor) {
        this.service = service;
        this.transformerRegistry = transformerRegistry;
        this.validator = validator;
        this.monitor = monitor;
    }

    protected JsonObject createDataSource(JsonObject dataSourceJson) {
        validator.validate(DATA_SOURCE_TYPE, dataSourceJson).orElseThrow(ValidationFailureException::new);

        var dataSource = transformerRegistry.transform(dataSourceJson, DataSource.class)
                .orElseThrow(InvalidRequestException::new);

        var created = service.create(dataSource)
                .orElseThrow(exceptionMapper(DataSource.class, dataSource.getId()));

        var idResponse = IdResponse.Builder.newInstance()
                .id(created.getId())
                .createdAt(created.getCreatedAt())
                .build();

        return transformerRegistry.transform(idResponse, JsonObject.class)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    protected void updateDataSource(JsonObject dataSourceJson) {
        validator.validate(DATA_SOURCE_TYPE, dataSourceJson).orElseThrow(ValidationFailureException::new);

        var dataSource = transformerRegistry.transform(dataSourceJson, DataSource.class)
                .orElseThrow(InvalidRequestException::new);

        service.update(dataSource)
                .orElseThrow(exceptionMapper(DataSource.class, dataSource.getId()));
    }

    protected JsonArray queryDataSources(JsonObject querySpecJson) {
        QuerySpec querySpec;
        if (querySpecJson == null) {
            querySpec = QuerySpec.max();
        } else {
            validator.validate(EDC_QUERY_SPEC_TYPE, querySpecJson).orElseThrow(ValidationFailureException::new);
            querySpec = transformerRegistry.transform(querySpecJson, QuerySpec.class)
                    .orElseThrow(InvalidRequestException::new);
        }

        return service.query(querySpec)
                .orElseThrow(exceptionMapper(QuerySpec.class, null)).stream()
                .map(it -> transformerRegistry.transform(it, JsonObject.class))
                .peek(r -> r.onFailure(f -> monitor.warning(f.getFailureDetail())))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(toJsonArray());
    }

    protected JsonObject getDataSource(String id) {
        var dataSource = service.findById(id)
                .orElseThrow(exceptionMapper(DataSource.class, id));

        return transformerRegistry.transform(dataSource, JsonObject.class)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    protected void deleteDataSource(String id) {
        service.deleteById(id)
                .orElseThrow(exceptionMapper(DataSource.class, id));
    }
}
