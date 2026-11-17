package eu.dataspace.connector.agreements.retirement.api;

import eu.dataspace.connector.agreements.retirement.api.model.AgreementsRetirementEntry;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.services.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;

import static eu.dataspace.connector.agreements.retirement.api.model.AgreementsRetirementEntry.AR_ENTRY_TYPE;
import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

/**
 * Legacy agreements retirement API, kept for backward compatibility. Every operation is delegated to the EDC
 * {@link ContractAgreementService}, which stores the retirement state on the {@link ContractAgreement} itself.
 *
 * @deprecated use the EDC contract agreement API: {@code POST /v4/contractagreements/{id}/retire},
 *         {@code POST /v4/contractagreements/{id}/unretire} and {@code POST /v4/contractagreements/request} filtering
 *         on {@code retired = true}.
 */
@Deprecated(since = "2.9.0")
public abstract class BaseAgreementsRetirementApiController {

    private static final String DEPRECATION_WARNING = "The '/contractagreements/retirements' endpoint is deprecated and will be removed, " +
            "please use '/v4/contractagreements/{id}/retire', '/v4/contractagreements/{id}/unretire' and '/v4/contractagreements/request' instead";

    protected final ContractAgreementService service;
    protected final TypeTransformerRegistry transformerRegistry;
    protected final JsonObjectValidatorRegistry validator;
    protected final Monitor monitor;

    public BaseAgreementsRetirementApiController(ContractAgreementService service, TypeTransformerRegistry transformerRegistry,
                                                 JsonObjectValidatorRegistry validator, Monitor monitor) {
        this.service = service;
        this.transformerRegistry = transformerRegistry;
        this.validator = validator;
        this.monitor = monitor;
    }

    protected JsonArray getAllRetired(JsonObject querySpecJson) {
        monitor.warning(DEPRECATION_WARNING);
        QuerySpec querySpec;
        if (querySpecJson == null) {
            querySpec = QuerySpec.max();
        } else {
            validator.validate(EDC_QUERY_SPEC_TYPE, querySpecJson).orElseThrow(ValidationFailureException::new);
            querySpec = transformerRegistry.transform(querySpecJson, QuerySpec.class)
                    .orElseThrow(InvalidRequestException::new);
        }

        return service.search(LegacyRetirementQuery.toContractAgreementQuery(querySpec))
                .orElseThrow(exceptionMapper(QuerySpec.class, null)).stream()
                .map(AgreementsRetirementEntry::from)
                .map(it -> transformerRegistry.transform(it, JsonObject.class))
                .peek(r -> r.onFailure(f -> monitor.warning(f.getFailureDetail())))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(toJsonArray());
    }

    protected void reactivateRetired(String agreementId) {
        monitor.warning(DEPRECATION_WARNING);
        var result = service.unretireAgreement(agreementId);
        if (result.failed() && result.reason() == ServiceFailure.Reason.CONFLICT) {
            // the legacy API answered 404 when the agreement was not retired, keep that contract
            throw new ObjectNotFoundException(AgreementsRetirementEntry.class, agreementId);
        }
        result.orElseThrow(exceptionMapper(ContractAgreement.class, agreementId));
    }

    protected void retireAgreement(JsonObject entry) {
        monitor.warning(DEPRECATION_WARNING);
        validator.validate(AR_ENTRY_TYPE, entry).orElseThrow(ValidationFailureException::new);

        var retirementEntry = transformerRegistry.transform(entry, AgreementsRetirementEntry.class)
                .orElseThrow(InvalidRequestException::new);

        service.retireAgreement(retirementEntry.agreementId(), retirementEntry.reason())
                .orElseThrow(exceptionMapper(ContractAgreement.class, retirementEntry.agreementId()));
    }
}
