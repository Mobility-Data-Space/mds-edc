package eu.dataspace.connector.extension.negotiation.manual.approval.api;

public interface ManualNegotiationApprovalApi {

    void approveNegotiation(String id);

    void rejectNegotiation(String id);
}
