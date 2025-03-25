package io.nexyo.edp.extensions.dtos.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Represents an Daseen resource.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DaseenResourceDto {

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("asset_id")
    private String assetId;

    @JsonProperty("resource_id")
    private String resourceId;

    @JsonProperty("contract_id")
    private String contractId;

    /**
     * Default constructor.
     */
    public DaseenResourceDto() {
        this.uuid = UUID.randomUUID().toString();
    }

    /**
     * Constructs an DaseenResourceDto with specified details.
     *
     * @param uuid       the unique identifier of the resource
     * @param assetId    the unique identifier of the resource
     * @param resourceId the unique identifier of the resource
     * @param contractId the current status of the resource
     */
    public DaseenResourceDto(String uuid, String assetId, String resourceId, String contractId) {
        this.uuid = uuid;
        this.assetId = assetId;
        this.resourceId = resourceId;
        this.contractId = contractId;
    }

    /**
     * Gets the unique identifier of the resource.
     *
     * @return the uuid
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * Sets the unique identifier of the resource.
     *
     * @param uuid the uuid to set
     */
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     * Retrieves the asset ID.
     *
     * @return the asset ID
     */
    public String getAssetId() {
        return assetId;
    }

    /**
     * Sets the asset ID.
     *
     * @param assetId the asset ID to set
     */
    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    /**
     * Retrieves the resource ID.
     *
     * @return the resource ID
     */
    public String getResourceId() {
        return resourceId;
    }

    /**
     * Sets the resource ID.
     *
     * @param resourceId the resource ID to set
     */
    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    /**
     * Retrieves the contract ID.
     *
     * @return the contract ID
     */
    public String getContractId() {
        return contractId;
    }

    /**
     * Sets the contract ID.
     *
     * @param contractId the contract ID to set
     */
    public void setContractId(String contractId) {
        this.contractId = contractId;
    }
}
