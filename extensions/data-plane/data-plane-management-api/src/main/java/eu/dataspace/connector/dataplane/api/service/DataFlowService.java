package eu.dataspace.connector.dataplane.api.service;

import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.controlplane.DataAddress;

import java.util.List;

/**
 * Read-only access to the data plane's {@link DataFlow} entities, exposed on the Management API. It is the V4
 * replacement for the EDR cache endpoints that were removed with Management API V3.
 */
public interface DataFlowService {

    /**
     * Returns all the data flows matching the given {@link QuerySpec}.
     *
     * @param querySpec the query to apply.
     * @return a list of the matching {@link DataFlow}.
     */
    ServiceResult<List<DataFlow>> findAll(QuerySpec querySpec);

    /**
     * Returns the {@link DataAddress} associated with the data flow identified by the given id. The address is resolved
     * from the {@code DataAddressStore} via the transfer process the flow originated from (their ids are aligned).
     *
     * @param dataFlowId the data flow id (which matches the transfer process id).
     * @return the {@link DataAddress}, or a not found failure if no transfer process or data address exists for it.
     */
    ServiceResult<DataAddress> getDataAddress(String dataFlowId);
}
