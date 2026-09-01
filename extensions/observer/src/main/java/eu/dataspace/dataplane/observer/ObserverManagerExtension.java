package eu.dataspace.dataplane.observer;

import eu.dataspace.dataplane.observer.store.ObserverEventStore;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.time.Clock;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

public class ObserverManagerExtension implements ServiceExtension {

    @Inject
    private Monitor monitor;
    @Inject
    private ContractNegotiationService negotiationService;
    @Inject
    private CatalogService catalogService;
    @Inject
    private SingleParticipantContextSupplier participantContextSupplier;
    @Inject
    private TypeManager typeManager;
    @Inject
    private TypeTransformerRegistry typeTransformerRegistry;
    @Inject
    private JsonLd jsonLd;
    @Inject
    private EventRouter eventRouter;
    @Inject
    private TransferProcessService transferProcessService;
    @Inject
    private Vault vault;
    @Inject
    private EdcHttpClient httpClient;
    @Inject
    private ObserverEventStore eventStore;
    @Inject
    private Clock clock;

    private ScheduledExecutorService retryExecutor;

    @Override
    public String name() {
        return "Observer Manager";
    }

    @Override
    public void shutdown() {
        if (retryExecutor != null) {
            retryExecutor.shutdownNow();
        }
    }

    @Provider
    public ObserverManager observerManager() {
        retryExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            var t = new Thread(r, "observer-event-retry");
            t.setDaemon(true);
            return t;
        });

        var registry = typeTransformerRegistry.forContext("dsp-api:2025-1");
        var observerNegotiationService = new ObserverNegotiationService(catalogService, registry, negotiationService,
                monitor, () -> typeManager.getMapper(JSON_LD), jsonLd);

        return new ObserverManager(
                monitor.withPrefix("[observer-manager]"),
                participantContextSupplier, () -> typeManager.getMapper(JSON_LD),
                eventRouter, transferProcessService, vault, httpClient, clock, eventStore, retryExecutor,
                observerNegotiationService);
    }
}
