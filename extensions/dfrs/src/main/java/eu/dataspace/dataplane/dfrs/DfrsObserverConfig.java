package eu.dataspace.dataplane.dfrs;

import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;

@Settings
public record DfrsObserverConfig(
        @Setting(key = "id", required = false,
                description = "The id of the participant responsible to observe DFRS events")
        String id,
        @Setting(key = "url", required = false,
                description = "The DSP url of the participant responsible to observe DFRS events")
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
        String transferProfile
) {
    public boolean isConfigured() {
        return id != null && url != null && datasetId != null && profile != null && transferProfile != null;
    }
}
