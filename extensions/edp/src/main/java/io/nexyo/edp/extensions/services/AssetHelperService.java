package io.nexyo.edp.extensions.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.nexyo.edp.extensions.exceptions.EdpException;
import org.eclipse.edc.connector.controlplane.services.spi.asset.AssetService;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.Optional;

/**
 * Service for handling assets and storing and loading information.
 */
public class AssetHelperService {

    public static final String EDPS_JOB_KEY = "edps_job";
    public static final String DASEEN_RESOURCE_KEY = "daseen_resource";
    private final ObjectMapper mapper = new ObjectMapper();
    private final AssetService assetService;
    private final Monitor logger;

    /**
     * Constructor for the AssetHelperService.
     *
     * @param assetService the asset service
     * @param monitor      the monitor
     */
    public AssetHelperService(AssetService assetService, Monitor monitor) {
        this.assetService = assetService;
        this.logger = monitor;
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Persists information on the asset.
     *
     * @param assetId the asset id
     * @param key     the key to store the data under
     * @param data    the data to store on the asset
     */
    private void persistRaw(String assetId, String key, String data) {
        var asset = this.assetService.findById(assetId);
        var updatedAsset = asset.toBuilder().property(key, data)
                .build();
        var result = assetService.update(updatedAsset);
        if (result.failed()) {
            this.logger.warning("Could not store information on asset: " + assetId);
        }
    }

    /**
     * Persists a DTO on the asset.
     *
     * @param <T>     the type of DTO to persist
     * @param assetId the asset id
     * @param key     the key to store the data under
     * @param clazz   the DTO to store
     */
    public <T> void persist(String assetId, String key, T clazz) {
        try {
            var serializedData = mapper.writeValueAsString(clazz);
            this.persistRaw(assetId, key, serializedData);
        } catch (JsonProcessingException e) {
            throw new EdpException("Unable to serialize " + clazz.getClass().getSimpleName(), e);
        }
    }

    /**
     * Retrieves stored information on the asset.
     *
     * @param assetId the asset id
     * @param key     the key to retrieve the data from
     * @return the stored information
     */
    public Optional<String> load(String assetId, String key) {
        var asset = this.assetService.findById(assetId);
        var jobId = asset.getProperty(key);

        if (jobId == null) {
            return Optional.empty();
        }

        return Optional.of(jobId.toString());
    }

    /**
     * Retrieves and deserializes a DTO from the asset.
     *
     * @param <T>     the type of DTO to deserialize
     * @param assetId the asset id
     * @param key     the key to retrieve the data from
     * @param clazz   the class of the DTO to deserialize
     * @return an Optional containing the deserialized DTO, or empty if not found
     */
    public <T> Optional<T> load(String assetId, String key, Class<T> clazz) {
        return this.load(assetId, key)
                .map(serializedData -> {
                    try {
                        return mapper.readValue(serializedData, clazz);
                    } catch (JsonProcessingException e) {
                        throw new EdpException("Unable to deserialize " + clazz.getSimpleName() + " from asset", e);
                    }
                });
    }
}
