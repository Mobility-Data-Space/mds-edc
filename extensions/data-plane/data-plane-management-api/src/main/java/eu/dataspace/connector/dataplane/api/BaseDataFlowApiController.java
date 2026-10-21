package eu.dataspace.connector.dataplane.api;

import eu.dataspace.connector.dataplane.api.service.DataFlowService;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

public abstract class BaseDataFlowApiController {

    protected final DataFlowService service;
    protected final TypeTransformerRegistry transformerRegistry;
    protected final JsonObjectValidatorRegistry validator;
    protected final Monitor monitor;

    public BaseDataFlowApiController(DataFlowService service, TypeTransformerRegistry transformerRegistry,
                                     JsonObjectValidatorRegistry validator, Monitor monitor) {
        this.service = service;
        this.transformerRegistry = transformerRegistry;
        this.validator = validator;
        this.monitor = monitor;
    }

    protected JsonArray requestDataFlows(JsonObject querySpecJson) {
        QuerySpec querySpec;
        if (querySpecJson == null) {
            querySpec = QuerySpec.max();
        } else {
            validator.validate(EDC_QUERY_SPEC_TYPE, querySpecJson).orElseThrow(ValidationFailureException::new);
            querySpec = transformerRegistry.transform(querySpecJson, QuerySpec.class)
                    .orElseThrow(InvalidRequestException::new);
        }

        return service.findAll(querySpec)
                .orElseThrow(exceptionMapper(QuerySpec.class, null)).stream()
                .map(it -> transformerRegistry.transform(it, JsonObject.class))
                .peek(r -> r.onFailure(f -> monitor.warning(f.getFailureDetail())))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(toJsonArray());
    }

    protected JsonObject getDataAddress(String id) {
        var dataAddress = service.getDataAddress(id)
                .orElseThrow(exceptionMapper(DataAddress.class, id));

        return transformerRegistry.transform(dataAddress, JsonObject.class)
                .orElseThrow(f -> new InvalidRequestException(f.getFailureDetail()));
    }
}
