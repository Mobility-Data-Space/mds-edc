package eu.dataspace.connector.agreements.retirement.service;

import eu.dataspace.connector.agreements.retirement.spi.service.EnhancedAgreementService;
import eu.dataspace.connector.agreements.retirement.spi.store.AgreementsRetirementStore;
import eu.dataspace.connector.agreements.retirement.spi.types.EnhancedContractAgreement;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.List;
import java.util.stream.Collectors;

public class EnhancedAgreementServiceImpl implements EnhancedAgreementService {

    private final AgreementsRetirementStore store;
    private final TransactionContext transactionContext;

    public EnhancedAgreementServiceImpl(AgreementsRetirementStore store, TransactionContext transactionContext) {
        this.store = store;
        this.transactionContext = transactionContext;
    }

    @Override
    public ServiceResult<List<EnhancedContractAgreement>> findAllAgreements(QuerySpec querySpec) {
        return transactionContext.execute(() -> ServiceResult.success(store.findEnhancedAgreements(querySpec).collect(Collectors.toList())));
    }

}
