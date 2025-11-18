package eu.dataspace.connector.agreements.retirement.defaults;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import eu.dataspace.connector.agreements.retirement.spi.store.AgreementsRetirementStore;

import static eu.dataspace.connector.agreements.retirement.AgreementsRetirementPreValidatorRegisterExtension.NAME;


@Extension(NAME)
public class DefaultAgreementRetirementStoreProviderExtension implements ServiceExtension {

    private static final String NAME = "Default Agreements Store Provider";

    @Override
    public String name() {
        return NAME;
    }

    @Inject
    CriterionOperatorRegistry criterionOperatorRegistry;

    @Provider(isDefault = true)
    public AgreementsRetirementStore createInMemStore() {
        return new InMemoryAgreementsRetirementStore(criterionOperatorRegistry);
    }

}
