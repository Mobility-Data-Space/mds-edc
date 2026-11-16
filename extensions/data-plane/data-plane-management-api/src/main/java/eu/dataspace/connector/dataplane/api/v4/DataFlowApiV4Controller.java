package eu.dataspace.connector.dataplane.api.v4;

import eu.dataspace.connector.dataplane.api.BaseDataFlowApiController;
import eu.dataspace.connector.dataplane.api.service.DataFlowService;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.validation.SchemaType;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE_TERM;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v4/dataflows")
public class DataFlowApiV4Controller extends BaseDataFlowApiController implements DataFlowApiV4 {

    public DataFlowApiV4Controller(DataFlowService service, TypeTransformerRegistry transformerRegistry, JsonObjectValidatorRegistry validator, Monitor monitor) {
        super(service, transformerRegistry, validator, monitor);
    }

    @POST
    @Path("/request")
    @Override
    public JsonArray requestDataFlowsV4(@SchemaType(value = EDC_QUERY_SPEC_TYPE_TERM, version = "v4") @RequestBody JsonObject querySpecJson) {
        return requestDataFlows(querySpecJson);
    }

    @GET
    @Path("/{id}/dataaddress")
    @Override
    public JsonObject getDataAddressV4(@PathParam("id") String id) {
        return getDataAddress(id);
    }
}
