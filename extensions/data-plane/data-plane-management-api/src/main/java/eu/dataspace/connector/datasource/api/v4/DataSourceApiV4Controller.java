package eu.dataspace.connector.datasource.api.v4;

import eu.dataspace.connector.datasource.api.BaseDataSourceApiController;
import eu.dataspace.connector.datasource.spi.service.DataSourceService;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.validation.SchemaType;

import static eu.dataspace.connector.datasource.spi.types.DataSource.DATA_SOURCE_TYPE_TERM;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE_TERM;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v4/datasources")
public class DataSourceApiV4Controller extends BaseDataSourceApiController implements DataSourceApiV4 {

    public DataSourceApiV4Controller(DataSourceService service, TypeTransformerRegistry transformerRegistry, JsonObjectValidatorRegistry validator, Monitor monitor) {
        super(service, transformerRegistry, validator, monitor);
    }

    @POST
    @Override
    public Response createDataSourceV4(@SchemaType(value = DATA_SOURCE_TYPE_TERM, version = "v4") @RequestBody JsonObject dataSource) {
        return Response.status(Response.Status.CREATED).entity(createDataSource(dataSource)).build();
    }

    @PUT
    @Override
    public void updateDataSourceV4(@SchemaType(value = DATA_SOURCE_TYPE_TERM, version = "v4") @RequestBody JsonObject dataSource) {
        updateDataSource(dataSource);
    }

    @POST
    @Path("/request")
    @Override
    public JsonArray requestDataSourcesV4(@SchemaType(value = EDC_QUERY_SPEC_TYPE_TERM, version = "v4") @RequestBody JsonObject querySpecJson) {
        return queryDataSources(querySpecJson);
    }

    @GET
    @Path("/{id}")
    @Override
    public JsonObject getDataSourceV4(@PathParam("id") String id) {
        return getDataSource(id);
    }

    @DELETE
    @Path("/{id}")
    @Override
    public void deleteDataSourceV4(@PathParam("id") String id) {
        deleteDataSource(id);
    }
}
