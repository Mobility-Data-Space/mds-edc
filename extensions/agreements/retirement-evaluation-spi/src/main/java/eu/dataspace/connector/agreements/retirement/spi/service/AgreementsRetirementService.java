package eu.dataspace.connector.agreements.retirement.spi.service;

import eu.dataspace.connector.agreements.retirement.spi.store.AgreementsRetirementStore;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import eu.dataspace.connector.agreements.retirement.spi.types.AgreementsRetirementEntry;

import java.util.List;

/**
 * Service interface that offers the necessary functionality for the Contract Agreement Retirement feature.
 */
public interface AgreementsRetirementService {

    /**
     * Given a contract agreement id, verifies if a corresponding {@link AgreementsRetirementEntry} exists in the {@link AgreementsRetirementStore}.
     *
     * @param agreementId the contract agreement id to verify
     * @return true if it exists, false otherwise.
     */
    boolean isRetired(String agreementId);

    /**
     * Returns a list of {@link AgreementsRetirementEntry} entries matching a valid {@link QuerySpec}
     *
     * @param querySpec a valid {@link QuerySpec}
     * @return a list of {@link AgreementsRetirementEntry}
     */
    ServiceResult<List<AgreementsRetirementEntry>> findAll(QuerySpec querySpec);

    /**
     * Saves an {@link AgreementsRetirementEntry} in the {@link AgreementsRetirementStore}.
     *
     * @param entry a valid {@link AgreementsRetirementEntry}
     * @return ServiceResult successs, or a conflict failure if it already exists.
     */
    ServiceResult<Void> retireAgreement(AgreementsRetirementEntry entry);

    /**
     * Given a contract agreement id, removes its matching {@link AgreementsRetirementEntry} from the {@link AgreementsRetirementStore}.
     *
     * @param contractAgreementId the contract agreement id of the AgreementRetirementEntry to delete
     * @return StoreResult success, not found failure if entry not found.
     */
    ServiceResult<Void> reactivate(String contractAgreementId);
}
