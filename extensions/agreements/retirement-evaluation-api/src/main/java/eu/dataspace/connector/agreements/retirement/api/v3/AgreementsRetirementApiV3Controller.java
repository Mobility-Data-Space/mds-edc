package eu.dataspace.connector.agreements.retirement.api.v3;

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
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import eu.dataspace.connector.agreements.retirement.api.BaseAgreementsRetirementApiController;
import eu.dataspace.connector.agreements.retirement.spi.service.AgreementsRetirementService;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v3/contractagreements/retirements")
public class AgreementsRetirementApiV3Controller extends BaseAgreementsRetirementApiController implements AgreementsRetirementApiV3 {

    public AgreementsRetirementApiV3Controller(AgreementsRetirementService service, TypeTransformerRegistry transformerRegistry, JsonObjectValidatorRegistry validator, Monitor monitor) {
        super(service, transformerRegistry, validator, monitor);
    }

    @POST
    @Path("/request")
    @Override
    public JsonArray getAllRetiredV3(@RequestBody JsonObject querySpecJson) {
        return getAllRetired(querySpecJson);
    }

    @DELETE
    @Path("/{agreementId}")
    @Override
    public void reactivateRetiredV3(@PathParam("agreementId") String agreementId) {
        reactivateRetired(agreementId);
    }

    @POST
    @Override
    public void retireAgreementV3(@RequestBody JsonObject entry) {
        retireAgreement(entry);
    }
}
