package eu.dataspace.connector.extension.negotiation.manual.approval.api.v4;

import eu.dataspace.connector.extension.negotiation.manual.approval.logic.ManualNegotiationApprovalService;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;

import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Path("/v4/contractnegotiations")
public class ManualNegotiationApprovalApiV4Controller implements ManualNegotiationApprovalApiV4 {

    private final ManualNegotiationApprovalService service;

    public ManualNegotiationApprovalApiV4Controller(ManualNegotiationApprovalService service) {
        this.service = service;
    }

    @POST
    @Path("/{id}/approve")
    @Override
    public void approveNegotiationV4(@PathParam("id") String id) {
        service.approve(id).orElseThrow(exceptionMapper(ContractNegotiation.class, id));
    }

    @POST
    @Path("/{id}/reject")
    @Override
    public void rejectNegotiationV4(@PathParam("id") String id) {
        service.reject(id).orElseThrow(exceptionMapper(ContractNegotiation.class, id));
    }
}
