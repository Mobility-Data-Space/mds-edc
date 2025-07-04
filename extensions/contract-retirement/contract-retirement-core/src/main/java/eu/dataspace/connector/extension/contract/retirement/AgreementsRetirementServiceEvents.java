            package eu.dataspace.connector.extension.contract.retirement;

import eu.dataspace.connector.extension.contract.retirement.event.ContractAgreementEvent;
import eu.dataspace.connector.extension.contract.retirement.event.ContractAgreementReactivated;
import eu.dataspace.connector.extension.contract.retirement.event.ContractAgreementRetired;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.tractusx.edc.agreements.retirement.spi.service.AgreementsRetirementService;
import org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry;

import java.time.Clock;
import java.util.List;

/**
 * Implementation for the {@link AgreementsRetirementService}.
 */
public class AgreementsRetirementServiceEvents implements AgreementsRetirementService {

    private final TransactionContext transactionContext;
    private final EventRouter eventRouter;
    private final Clock clock;
    private final AgreementsRetirementService originalService;

    public AgreementsRetirementServiceEvents(TransactionContext transactionContext,
                                             EventRouter eventRouter,
                                             Clock clock, AgreementsRetirementService originalService) {
        this.transactionContext = transactionContext;
        this.eventRouter = eventRouter;
        this.clock = clock;
        this.originalService = originalService;
    }

    @Override
    public boolean isRetired(String agreementId) {
        return originalService.isRetired(agreementId);
    }

    @Override
    public ServiceResult<List<AgreementsRetirementEntry>> findAll(QuerySpec querySpec) {
        return originalService.findAll(querySpec);
    }

    @Override
    public ServiceResult<Void> retireAgreement(AgreementsRetirementEntry entry) {
        return transactionContext.execute(() -> originalService.retireAgreement(entry)
                .onSuccess(v -> publish(ContractAgreementRetired.Builder.newInstance()
                        .contractAgreementId(entry.getAgreementId()).build()))
        );
    }

    @Override
    public ServiceResult<Void> reactivate(String contractAgreementId) {
        return transactionContext.execute(() -> originalService.reactivate(contractAgreementId)
                .onSuccess(v -> publish(ContractAgreementReactivated.Builder.newInstance()
                        .contractAgreementId(contractAgreementId).build()))
        );
    }

    private void publish(ContractAgreementEvent event) {
        eventRouter.publish(EventEnvelope.Builder.newInstance().at(clock.millis()).payload(event).build());
    }

}
