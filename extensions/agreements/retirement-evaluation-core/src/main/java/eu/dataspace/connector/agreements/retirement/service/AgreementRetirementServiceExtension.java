package eu.dataspace.connector.agreements.retirement.service;

import eu.dataspace.connector.agreements.retirement.spi.service.AgreementsRetirementService;
import eu.dataspace.connector.agreements.retirement.spi.service.EnhancedAgreementService;
import eu.dataspace.connector.agreements.retirement.spi.store.AgreementsRetirementStore;
import org.eclipse.edc.connector.controlplane.services.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.time.Clock;

import static eu.dataspace.connector.agreements.retirement.AgreementsRetirementPreValidatorRegisterExtension.NAME;

@Extension(NAME)
public class AgreementRetirementServiceExtension implements ServiceExtension {

    private static final String NAME = "Agreement Retirement Service Extension";

    @Inject
    AgreementsRetirementStore store;
    @Inject
    TransactionContext transactionContext;
    @Inject
    ContractAgreementService contractAgreementService;
    @Inject
    EventRouter eventRouter;
    @Inject
    Clock clock;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public AgreementsRetirementService agreementsRetirementService() {
        return new AgreementsRetirementServiceImpl(store, transactionContext, contractAgreementService, eventRouter, clock);
    }

    @Provider
    public EnhancedAgreementService enhancedAgreementService() {
        return new EnhancedAgreementServiceImpl(store, transactionContext);
    }
}
