package eu.dataspace.dataplane.observer;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dataspace.connector.agreements.retirement.spi.event.ContractAgreementRetired;
import eu.dataspace.dataplane.observer.model.event.ObserverEventStored;
import eu.dataspace.dataplane.observer.store.ObserverEventStore;
import eu.dataspace.dataplane.observer.subscriber.DispatchObserverEvent;
import eu.dataspace.dataplane.observer.subscriber.ObserverEventDispatcher;
import eu.dataspace.dataplane.observer.subscriber.ObserverEventRetryJob;
import eu.dataspace.dataplane.observer.subscriber.ReNegotiateObserver;
import eu.dataspace.dataplane.observer.subscriber.StartObserverTransfer;
import eu.dataspace.dataplane.observer.subscriber.StoreObserverAddress;
import eu.dataspace.dataplane.observer.subscriber.StoreObserverEvent;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationFinalized;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessTerminated;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextSupplier;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;

import java.time.Clock;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;


public class ObserverManager {

    public static final String OBSERVER_ADDRESS_KEY = "observer-address";

    private final Monitor monitor;
    private final ParticipantContextSupplier participantContextSupplier;
    private final Supplier<ObjectMapper> mapperSupplier;
    private final EventRouter eventRouter;
    private final TransferProcessService transferProcessService;
    private final Vault vault;
    private final EdcHttpClient httpClient;
    private final Clock clock;
    private final ObserverEventStore eventStore;
    private final ScheduledExecutorService retryExecutor;
    private final ObserverNegotiationService observerNegotiationService;

    public ObserverManager(Monitor monitor,
                           ParticipantContextSupplier participantContextSupplier,
                           Supplier<ObjectMapper> mapperSupplier,
                           EventRouter eventRouter, TransferProcessService transferProcessService, Vault vault,
                           EdcHttpClient httpClient, Clock clock, ObserverEventStore eventStore,
                           ScheduledExecutorService retryExecutor, ObserverNegotiationService observerNegotiationService) {
        this.monitor = monitor;
        this.participantContextSupplier = participantContextSupplier;
        this.mapperSupplier = mapperSupplier;
        this.eventRouter = eventRouter;
        this.transferProcessService = transferProcessService;
        this.vault = vault;
        this.httpClient = httpClient;
        this.clock = clock;
        this.eventStore = eventStore;
        this.retryExecutor = retryExecutor;
        this.observerNegotiationService = observerNegotiationService;
    }

    public Result<Void> activate(ObserverConfig configuration) {
        var participantContextServiceResult = participantContextSupplier.get();
        if (participantContextServiceResult.failed()) {
            return Result.failure("Cannot obtain ParticipantContextSupplier: " + participantContextServiceResult.getFailureDetail());
        }

        var participantContext = participantContextServiceResult.getContent();

        eventRouter.register(ContractNegotiationFinalized.class,
                new StartObserverTransfer(configuration, participantContext, transferProcessService, monitor));

        eventRouter.register(TransferProcessStarted.class,
                new StoreObserverAddress(configuration, participantContext, mapperSupplier, vault, monitor));

        var retryInterval = configuration.retryInterval();
        var storeObserverEvent = new StoreObserverEvent(participantContext, mapperSupplier, eventStore, eventRouter, clock, retryInterval);
        eventRouter.registerSync(ContractNegotiationFinalized.class, storeObserverEvent);
        eventRouter.registerSync(TransferProcessStarted.class, storeObserverEvent);
        eventRouter.registerSync(ContractAgreementRetired.class, storeObserverEvent);

        var dispatcher = new ObserverEventDispatcher(mapperSupplier, httpClient, vault, participantContext);
        eventRouter.register(ObserverEventStored.class, new DispatchObserverEvent(eventStore, dispatcher, monitor, clock, retryInterval));

        var retryJob = new ObserverEventRetryJob(eventStore, dispatcher, monitor, clock, retryInterval);
        retryExecutor.scheduleWithFixedDelay(retryJob, 0, retryInterval.getSeconds(), TimeUnit.SECONDS);

        eventRouter.register(TransferProcessTerminated.class, new ReNegotiateObserver(configuration, participantContext, transferProcessService, observerNegotiationService));

        if (observerNegotiationService.isNegotiationAlreadySetup(configuration)) {
            monitor.info("DFRS Observer agreement already set up");
            return Result.success();
        }

        observerNegotiationService.negotiateObserverOffer(configuration, participantContext);

        return Result.success();
    }

}
