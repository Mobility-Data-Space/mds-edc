package eu.dataspace.connector.agreements.retirement.defaults;

import eu.dataspace.connector.agreements.retirement.spi.store.AgreementsRetirementStore;
import eu.dataspace.connector.agreements.retirement.store.AgreementsRetirementStoreTestBase;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.defaults.storage.contractnegotiation.InMemoryContractNegotiationStore;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;

import java.time.Clock;


public class InMemoryAgreementsRetirementStoreTest extends AgreementsRetirementStoreTestBase {

    private final CriterionOperatorRegistry criterionOperatorRegistry = CriterionOperatorRegistryImpl.ofDefaults();
    private final InMemoryContractNegotiationStore contractNegotiationStore = new InMemoryContractNegotiationStore(Clock.systemDefaultZone(), criterionOperatorRegistry);
    private final InMemoryAgreementsRetirementStore store = new InMemoryAgreementsRetirementStore(criterionOperatorRegistry, contractNegotiationStore);

    @Override
    protected AgreementsRetirementStore getStore() {
        return store;
    }

    @Override
    protected ContractNegotiationStore getContractNegotiationStore() {
        return contractNegotiationStore;
    }
}
