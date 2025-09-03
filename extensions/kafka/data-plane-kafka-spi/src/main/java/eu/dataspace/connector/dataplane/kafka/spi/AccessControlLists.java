package eu.dataspace.connector.dataplane.kafka.spi;

import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;

/**
 * Manages access control on Kafka
 */
    public interface AccessControlLists {

    /**
     * Allow access to a user
     *
     * @param principalName the principal name
     * @param dataFlowId the data flow id
     * @param dataAddress the source data address
     * @return response
     */
    ServiceResult<AllowResponse> allowAccessTo(String principalName, String dataFlowId, DataAddress dataAddress);


    /**
     * Deny access to a user linked to a data flow
     *
     * @param dataFlowId the data flow id
     * @return response
     */
    ServiceResult<Void> denyAccessTo(String dataFlowId);
}
