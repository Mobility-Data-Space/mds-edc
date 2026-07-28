package eu.dataspace.connector.agreements.retirement.api;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;
import eu.dataspace.connector.agreements.retirement.spi.service.AgreementsRetirementService;
import eu.dataspace.connector.agreements.retirement.spi.types.AgreementsRetirementEntry;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;
import static eu.dataspace.connector.agreements.retirement.spi.types.AgreementsRetirementEntry.AR_ENTRY_TYPE;

public abstract class BaseAgreementsRetirementApiController {

    protected final AgreementsRetirementService service;
    protected final TypeTransformerRegistry transformerRegistry;
    protected final JsonObjectValidatorRegistry validator;
    protected final Monitor monitor;

    public BaseAgreementsRetirementApiController(AgreementsRetirementService service, TypeTransformerRegistry transformerRegistry,
                                                  JsonObjectValidatorRegistry validator, Monitor monitor) {
        this.service = service;
        this.transformerRegistry = transformerRegistry;
        this.validator = validator;
        this.monitor = monitor;
    }

    protected JsonArray getAllRetired(JsonObject querySpecJson) {
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

    protected void reactivateRetired(String agreementId) {
        service.reactivate(agreementId)
                .orElseThrow(exceptionMapper(AgreementsRetirementEntry.class, agreementId));
    }

    protected void retireAgreement(JsonObject entry) {
        validator.validate(AR_ENTRY_TYPE, entry).orElseThrow(ValidationFailureException::new);

        var retirementEntry = transformerRegistry.transform(entry, AgreementsRetirementEntry.class)
                .orElseThrow(InvalidRequestException::new);

        service.retireAgreement(retirementEntry)
                .orElseThrow(exceptionMapper(AgreementsRetirementEntry.class, retirementEntry.getAgreementId()));
    }
}
