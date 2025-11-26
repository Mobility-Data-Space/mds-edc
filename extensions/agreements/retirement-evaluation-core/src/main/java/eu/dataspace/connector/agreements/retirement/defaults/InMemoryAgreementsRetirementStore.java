package eu.dataspace.connector.agreements.retirement.defaults;

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
    private final Map<String, AgreementsRetirementEntry> cache = new ConcurrentHashMap<>();

    public InMemoryAgreementsRetirementStore(CriterionOperatorRegistry criterionOperatorRegistry) {
        queryResolver = new ReflectionBasedQueryResolver<>(AgreementsRetirementEntry.class, criterionOperatorRegistry);
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
}
