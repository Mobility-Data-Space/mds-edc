package eu.dataspace.connector.agreements.retirement.api.v4;

import eu.dataspace.connector.agreements.retirement.api.BaseAgreementsRetirementApiController;
import eu.dataspace.connector.agreements.retirement.spi.service.AgreementsRetirementService;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.validation.SchemaType;

import static eu.dataspace.connector.agreements.retirement.spi.types.AgreementsRetirementEntry.AGREEMENTS_RETIREMENT_ENTRY_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE_TERM;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v4/contractagreements/retirements")
public class AgreementsRetirementApiV4Controller extends BaseAgreementsRetirementApiController implements AgreementsRetirementApiV4 {

    public AgreementsRetirementApiV4Controller(AgreementsRetirementService service, TypeTransformerRegistry transformerRegistry, JsonObjectValidatorRegistry validator, Monitor monitor) {
        super(service, transformerRegistry, validator, monitor);
    }

    @POST
    @Path("/request")
    @Override
    public JsonArray getAllRetiredV4(@SchemaType(EDC_QUERY_SPEC_TYPE_TERM) @RequestBody JsonObject querySpecJson) {
        return getAllRetired(querySpecJson);
    }

    @DELETE
    @Path("/{agreementId}")
    @Override
    public void reactivateRetiredV4(@PathParam("agreementId") String agreementId) {
        reactivateRetired(agreementId);
    }

    @POST
    @Override
    public void retireAgreementV4(@SchemaType(AGREEMENTS_RETIREMENT_ENTRY_TYPE) @RequestBody JsonObject entry) {
        retireAgreement(entry);
    }
}
