package eu.dataspace.connector.agreements.retirement.spi.store;

import eu.dataspace.connector.agreements.retirement.spi.types.EnhancedContractAgreement;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import eu.dataspace.connector.agreements.retirement.spi.types.AgreementsRetirementEntry;

import java.util.stream.Stream;

/**
 * Interface for managing the storage point of {@link AgreementsRetirementEntry}.
 */
@ExtensionPoint
public interface AgreementsRetirementStore  {
    String NOT_FOUND_IN_RETIREMENT_TEMPLATE = "Contract Agreement with %s was not found on retirement table.";
    String NOT_FOUND_IN_CONTRACT_AGREEMENT_TEMPLATE = "Contract Agreement with %s was not found on contract agreement table.";
    String ALREADY_EXISTS_TEMPLATE = "Contract Agreement %s is already retired.";

    /**
     * Saves an AgreementsRetirementEntry in the store.
     *
     * @param entry {@link AgreementsRetirementEntry}
     * @return StoreResult success, already exists failure if already exists
     */
    StoreResult<Void> save(AgreementsRetirementEntry entry);

    /**
     * Deletes an AgreementsRetirementEntry from the store, given a contract agreement id.
     *
     * @param contractAgreementId the contract agreement id of the AgreementRetirementEntry to delete
     * @return StoreResult success, not found failure if entry not found.
     */
    StoreResult<Void> delete(String contractAgreementId);

    /**
     * Returns a list of AgreementRetirementEntry matching a query spec.
     *
     * @param querySpec a valid {@link QuerySpec}
     * @return a list of AgreementRetirementEntry entries.
     */
    Stream<AgreementsRetirementEntry> findRetiredAgreements(QuerySpec querySpec);

    /**
     * Returns the agreements with retirement info
     *
     * @param querySpec the query spec
     * @return the list of agreements with retirement info
     */
    Stream<EnhancedContractAgreement> findEnhancedAgreements(QuerySpec querySpec);
}
