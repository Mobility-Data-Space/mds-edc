package eu.dataspace.connector.extension.negotiation.manual.approval.api.v4;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.tags.Tag;

@OpenAPIDefinition(info = @Info(version = "v4"))
@Tag(
        name = "Manual Negotiation Approval v4",
        description = "Permits to manually approve or reject contract negotiations in pending state"
)
public interface ManualNegotiationApprovalApiV4 {

    @Operation(
            description = "Approve a pending negotiation"
    )
    void approveNegotiationV4(String id);

    @Operation(
            description = "Reject a pending negotiation"
    )
    void rejectNegotiationV4(String id);
}
