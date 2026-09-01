package eu.dataspace.dataplane.observer;

import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;

import java.time.Duration;

@Settings
public record ObserverConfig(
        @Setting(key = "id", required = false,
                description = "The id of the participant responsible to observe events")
        String id,
        @Setting(key = "url", required = false,
                description = "The DSP url of the participant responsible to observe events")
        String url,
        @Setting(key = "dataset.id", required = false,
                description = "The dataset ID on which the observe offer will be exposed in the dataspace")
        String datasetId,
        @Setting(key = "profile", required = false,
                description = "The profile used for the communication."
        )
        String profile,
        @Setting(key = "transfer.profile", required = false,
                description = "Tre transfer profile used in DPS communication."
        )
        String transferProfile,
        @Setting(key = "retry.interval", required = false, defaultValue = "PT30S",
                description = "Interval in ISO-8061 duration format between retry attempts for failed observer event dispatches. Also used as the base for exponential backoff."
        )
        Duration retryInterval
) {
    public boolean isConfigured() {
        return id != null && url != null && datasetId != null && profile != null && transferProfile != null;
    }
}
