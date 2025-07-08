package eu.dataspace.connector.extension.negotiation.manual.approval.api;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@OpenAPIDefinition
@Tag(
        name = "Manual Negotiation Approval",
        description = "Permits to manually approve or reject contract negotiations in pending state"
)
public interface ManualNegotiationApprovalApi {

    @Operation(
            description = "Approve a pending negotiation"
    )
    void approveNegotiation(String id);


    @Operation(
            description = "Reject a pending negotiation"
    )
    void rejectNegotiation(String id);
}
