package eu.dataspace.connector.agreements.retirement.defaults;

import eu.dataspace.connector.agreements.retirement.spi.types.EnhancedContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.store.ReflectionBasedQueryResolver;
import eu.dataspace.connector.agreements.retirement.spi.store.AgreementsRetirementStore;
import eu.dataspace.connector.agreements.retirement.spi.types.AgreementsRetirementEntry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * In Memory implementation of a {@link AgreementsRetirementStore}.
 */
public class InMemoryAgreementsRetirementStore implements AgreementsRetirementStore {

    private final QueryResolver<AgreementsRetirementEntry> queryResolver;
    private final ContractNegotiationStore contractNegotiationStore;
    private final Map<String, AgreementsRetirementEntry> cache = new ConcurrentHashMap<>();

    public InMemoryAgreementsRetirementStore(CriterionOperatorRegistry criterionOperatorRegistry, ContractNegotiationStore contractNegotiationStore) {
        queryResolver = new ReflectionBasedQueryResolver<>(AgreementsRetirementEntry.class, criterionOperatorRegistry);
        this.contractNegotiationStore = contractNegotiationStore;
    }

    @Override
    public StoreResult<Void> save(AgreementsRetirementEntry entry) {
        if (cache.containsKey(entry.getAgreementId())) {
            return StoreResult.alreadyExists(ALREADY_EXISTS_TEMPLATE.formatted(entry.getAgreementId()));
        }
        cache.put(entry.getAgreementId(), entry);
        return StoreResult.success();
    }

    @Override
    public StoreResult<Void> delete(String contractAgreementId) {
        return cache.remove(contractAgreementId) == null ?
                StoreResult.notFound(NOT_FOUND_IN_RETIREMENT_TEMPLATE.formatted(contractAgreementId)) :
                StoreResult.success();
    }

    @Override
    public Stream<AgreementsRetirementEntry> findRetiredAgreements(QuerySpec querySpec) {
        return queryResolver.query(cache.values().stream(), querySpec);
    }

    @Override
    public Stream<EnhancedContractAgreement> findEnhancedAgreements(QuerySpec querySpec) {
        return contractNegotiationStore.queryAgreements(querySpec)
                .map(agreement -> new EnhancedContractAgreement(agreement, cache.get(agreement.getId())));
    }
}
