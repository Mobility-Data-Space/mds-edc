package eu.dataspace.connector.agreements.retirement.spi.service;

import eu.dataspace.connector.agreements.retirement.spi.types.EnhancedContractAgreement;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.List;

/**
 * Service interface that offers enhanced contract agreements
 */
public interface EnhancedAgreementService {

    /**
     * Returns the ContractAgreementWithRetirement according to the passed query
     *
     * @param querySpec the query.
     * @return the contracts agreement with retirement info
     */
    ServiceResult<List<EnhancedContractAgreement>> findAllAgreements(QuerySpec querySpec);
}
