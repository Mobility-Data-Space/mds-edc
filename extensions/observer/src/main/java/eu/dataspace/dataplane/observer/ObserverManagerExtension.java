package eu.dataspace.dataplane.observer;

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
    private Clock clock;

    @Override
    public String name() {
        return "Observer Manager";
    }

    @Provider
    public ObserverManager observerManager() {
        return new ObserverManager(
                monitor.withPrefix("[observer-manager]"),
                negotiationService,
                typeTransformerRegistry.forContext("dsp-api:2025-1"),
                participantContextSupplier, catalogService, jsonLd, () -> typeManager.getMapper(JSON_LD),
                eventRouter, transferProcessService, vault, httpClient, clock
        );
    }
}
