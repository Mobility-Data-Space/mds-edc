package eu.dataspace.connector.extension.contract.retirement;

import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.tractusx.edc.agreements.retirement.spi.service.AgreementsRetirementService;

import java.time.Clock;

/**
 * This extension provides a workaround to get retirement events. The functionality has already been implemented in
 * Tractus-X EDC, but it will available since version 0.11.0, so this extension can be removed when the new version of
 * Tractus-x EDC will be used as dependency.
 */
@Deprecated(since = "1.0.0")
public class ContractRetirementExtension implements ServiceExtension {

    @Inject
    private EventRouter eventRouter;
    @Inject
    private AgreementsRetirementService agreementsRetirementService;
    @Inject
    private TransactionContext transactionContext;
    @Inject
    private Clock clock;

    @Override
    public void initialize(ServiceExtensionContext context) {
        context.registerService(AgreementsRetirementService.class, new AgreementsRetirementServiceEvents(transactionContext, eventRouter, clock, agreementsRetirementService));
    }

}
