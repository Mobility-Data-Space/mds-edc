package eu.dataspace.connector.agreements.retirement.defaults;

import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import eu.dataspace.connector.agreements.retirement.spi.store.AgreementsRetirementStore;
import eu.dataspace.connector.agreements.retirement.store.AgreementsRetirementStoreTestBase;


public class InMemoryAgreementsRetirementStoreTest extends AgreementsRetirementStoreTestBase {

    private final InMemoryAgreementsRetirementStore store = new InMemoryAgreementsRetirementStore(CriterionOperatorRegistryImpl.ofDefaults());

    @Override
    protected AgreementsRetirementStore getStore() {
        return store;
    }
}
