package eu.dataspace.connector.dataplane.kafka.spi;

import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;

/**
 * Provide and revoke access through IdP for a specific source address
 */
public interface IdentityProvider {

    /**
     * Grant access
     *
     * @param dataFlowId the data flow id
     * @param dataAddress the data address
     * @return result
     */
    ServiceResult<Credentials> grantAccess(String dataFlowId, DataAddress dataAddress);

    /**
     * Revoke access
     *
     * @param dataFlowId the data flow id
     * @return result
     */
    ServiceResult<Void> revokeAccess(String dataFlowId);

}
