package eu.dataspace.connector.agreements.retirement.store;

import eu.dataspace.connector.agreements.retirement.spi.store.AgreementsRetirementStore;
import eu.dataspace.connector.agreements.retirement.spi.types.AgreementsRetirementEntry;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.stream.Collectors;

import static eu.dataspace.connector.agreements.retirement.spi.store.AgreementsRetirementStore.ALREADY_EXISTS_TEMPLATE;
import static eu.dataspace.connector.agreements.retirement.spi.store.AgreementsRetirementStore.NOT_FOUND_IN_RETIREMENT_TEMPLATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

public abstract class AgreementsRetirementStoreTestBase {

    @Test
    void findRetiredAgreement() {
        var agreementId = "test-agreement-id";
        var entry = createRetiredAgreementEntry(agreementId, "mock-reason");
        getStore().save(entry);

        var query = createFilterQueryByAgreementId(agreementId);
        var retiredAgreements = getStore().findRetiredAgreements(query).collect(Collectors.toList());
        assertThat(retiredAgreements)
                .isNotNull()
                .hasSize(1)
                .first()
                .extracting(AgreementsRetirementEntry::getAgreementId)
                .isEqualTo(agreementId);
    }

    @Test
    void findRetiredAgreement_notExists() {
        var agreementId = "test-agreement-not-exists";
        var query = createFilterQueryByAgreementId(agreementId);
        var result = getStore().findRetiredAgreements(query).collect(Collectors.toList());
        assertThat(result).isEmpty();
    }

    @Test
    void save_whenExists() {
        var agreementId = "test-agreement-id";
        var entry = createRetiredAgreementEntry(agreementId, "mock-reason");
        getStore().save(entry);
        var result = getStore().save(entry);
        assertThat(result).isFailed()
                .detail().isEqualTo(ALREADY_EXISTS_TEMPLATE.formatted(agreementId));
    }

    @Test
    void delete() {
        var agreementId = "test-agreement-id";
        var entry = createRetiredAgreementEntry(agreementId, "mock-reason");
        getStore().save(entry);
        var delete = getStore().delete(agreementId);

        assertThat(delete).isSucceeded();
    }

    @Test
    void delete_notExist() {
        var agreementId = "test-agreement-id";
        var delete = getStore().delete(agreementId);

        assertThat(delete).isFailed()
                .detail().isEqualTo(NOT_FOUND_IN_RETIREMENT_TEMPLATE.formatted(agreementId));
    }

    @Nested
    class FindAgreements {
        @Test
        void shouldReturnEmptyList_whenThereAreNoAgreements() {
            var agreements = getStore().findEnhancedAgreements(QuerySpec.max());

            assertThat(agreements).isEmpty();
        }

        @Test
        void shouldReturnAgreementWithoutRevocation() {
            var contractNegotiation = createNegotiationWithAgreement();

            getContractNegotiationStore().save(contractNegotiation);

            var agreements = getStore().findEnhancedAgreements(QuerySpec.max());

            assertThat(agreements).hasSize(1).first().satisfies(enriched -> {
                assertThat(enriched.agreement()).usingRecursiveComparison().isEqualTo(contractNegotiation.getContractAgreement());
            });
        }

        @Test
        void shouldReturnAgreementWithRevocation() {
            var contractNegotiation = createNegotiationWithAgreement();
            getContractNegotiationStore().save(contractNegotiation);
            var retirementEntry = createRetiredAgreementEntry(contractNegotiation.getContractAgreement().getId(), "mock-reason");
            getStore().save(retirementEntry);

            var agreements = getStore().findEnhancedAgreements(QuerySpec.max());

            assertThat(agreements).hasSize(1).first().satisfies(enriched -> {
                assertThat(enriched.agreement()).usingRecursiveComparison().isEqualTo(contractNegotiation.getContractAgreement());
                assertThat(enriched.retirement()).usingRecursiveComparison().ignoringFields("createdAt").isEqualTo(retirementEntry);
            });
        }

        private ContractNegotiation createNegotiationWithAgreement() {
            var contractAgreement = ContractAgreement.Builder.newInstance()
                    .agreementId(UUID.randomUUID().toString())
                    .providerId(UUID.randomUUID().toString())
                    .consumerId(UUID.randomUUID().toString())
                    .assetId(UUID.randomUUID().toString())
                    .policy(Policy.Builder.newInstance().build())
                    .build();

            return ContractNegotiation.Builder.newInstance()
                    .counterPartyId(UUID.randomUUID().toString())
                    .counterPartyAddress(UUID.randomUUID().toString())
                    .protocol(UUID.randomUUID().toString())
                    .contractAgreement(contractAgreement).build();
        }
    }

    private AgreementsRetirementEntry createRetiredAgreementEntry(String agreementId, String reason) {
        return AgreementsRetirementEntry.Builder.newInstance()
                .withAgreementId(agreementId)
                .withReason(reason)
                .build();
    }

    private QuerySpec createFilterQueryByAgreementId(String agreementId) {
        return QuerySpec.Builder.newInstance()
                .filter(
                        Criterion.Builder.newInstance()
                                .operandLeft("agreementId")
                                .operator("=")
                                .operandRight(agreementId)
                                .build()
                ).build();
    }

    protected abstract AgreementsRetirementStore getStore();

    protected abstract ContractNegotiationStore getContractNegotiationStore();

}
