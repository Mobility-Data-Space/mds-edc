package eu.dataspace.connector.extension.logginghouse.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.truzzt.extension.logginghouse.client.events.CustomLoggingHouseEvent;
import eu.dataspace.connector.extension.contract.retirement.event.ContractAgreementEvent;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import java.time.Clock;
import java.util.UUID;

public class LoggingHousePublisherExtension implements ServiceExtension {

    @Inject
    private EventRouter eventRouter;
    @Inject
    private TypeManager typeManager;
    @Inject
    private Clock clock;
    @Inject
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        eventRouter.registerSync(ContractAgreementEvent.class, new WrapContractAgreementEvents());
    }

    private class WrapContractAgreementEvents implements EventSubscriber {
        @Override
        public <E extends Event> void on(EventEnvelope<E> event) {
            var payload = event.getPayload();

            var wrappedEvent = createWrapperEvent(payload);
            if (wrappedEvent != null) {
                var envelope = EventEnvelope.Builder
                        .newInstance()
                        .payload(wrappedEvent)
                        .at(clock.millis())
                        .build();
                eventRouter.publish(envelope);
            }
        }

        private <E extends Event> CustomLoggingHouseEventWrapper createWrapperEvent(E payload) {
            if (payload instanceof ContractAgreementEvent agreementEvent) {
                var serialized = serialize(agreementEvent);
                return new CustomLoggingHouseEventWrapper(agreementEvent.getContractAgreementId(), serialized);
            }
            return null;
        }

        private String serialize(Event event) {
            try {
                ObjectNode tree = typeManager.getMapper().valueToTree(event);
                tree.put("className", event.getClass().getSimpleName());
                return typeManager.getMapper().writeValueAsString(tree);
            } catch (JsonProcessingException e) {
                var message = "Cannot serialize event " + event;
                monitor.severe(message);
                throw new EdcException(message, e);
            }
        }
    }

    private static class CustomLoggingHouseEventWrapper extends CustomLoggingHouseEvent {

        private final String eventId;
        private final String processId;
        private final String messageBody;

        private CustomLoggingHouseEventWrapper(String processId, String messageBody) {
            this.eventId = UUID.randomUUID().toString();
            this.processId = processId;
            this.messageBody = messageBody;
        }

        @Override
        public String getEventId() {
            return eventId;
        }

        @Override
        public String getProcessId() {
            return processId;
        }

        @Override
        public String getMessageBody() {
            return messageBody;
        }

    }

}
