package eu.dataspace.dataplane.dfrs.model;

import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;

import java.time.Clock;
import java.util.UUID;

public record ObserverEventEnvelope(
        String specversion,
        String id,
        String source,
        String type,
        String time,
        String datacontenttype,
        ObserverEvent data
) {

    private static final String SPEC_VERSION = "1.0";
    private static final String APPLICATION_JSON = "application/json";

    public static ObserverEventEnvelope create(ObserverEvent event, ParticipantContext participantContext, Clock clock) {
        return new ObserverEventEnvelope(
                SPEC_VERSION,
                UUID.randomUUID().toString(),
                participantContext.getIdentity(),
                event.eventType(),
                clock.instant().toString(),
                APPLICATION_JSON,
                event
        );
    }
}
