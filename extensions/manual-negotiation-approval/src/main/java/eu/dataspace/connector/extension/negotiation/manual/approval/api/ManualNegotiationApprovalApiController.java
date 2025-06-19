package eu.dataspace.connector.extension.negotiation.manual.approval.api;

import eu.dataspace.connector.extension.negotiation.manual.approval.logic.ManualNegotiationApprovalService;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;

import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Path("/v3/contractnegotiations")
public class ManualNegotiationApprovalApiController implements ManualNegotiationApprovalApi {

    private final ManualNegotiationApprovalService service;

    public ManualNegotiationApprovalApiController(ManualNegotiationApprovalService service) {
        this.service = service;
    }

    @POST
    @Path("/{id}/approve")
    @Override
    public void approveNegotiation(@PathParam("id") String id) {
        service.approve(id).orElseThrow(exceptionMapper(ContractNegotiation.class, id));
    }

    @POST
    @Path("/{id}/reject")
    @Override
    public void rejectNegotiation(@PathParam("id") String id) {
        service.reject(id).orElseThrow(exceptionMapper(ContractNegotiation.class, id));
    }
}
