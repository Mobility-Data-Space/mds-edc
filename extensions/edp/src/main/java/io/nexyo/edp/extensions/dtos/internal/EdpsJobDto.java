package io.nexyo.edp.extensions.dtos.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Represents an EDPS job.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EdpsJobDto {

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("asset_id")
    private String assetId;

    @JsonProperty("job_id")
    private String jobUuid;

    @JsonProperty("state")
    private String state;

    @JsonProperty("state_detail")
    private String details;

    @JsonProperty("contract_id")
    private String contractId;

    @JsonProperty("upload_url")
    private String uploadUrl;

    @JsonProperty("result_url")
    private String resultUrl;

    /**
     * Default constructor.
     */
    public EdpsJobDto() {
        this.uuid = UUID.randomUUID().toString();
    }

    /**
     * Constructs an EdpsJobModel with specified details.
     *
     * @param uuid     the unique identifier of the job model
     * @param jobUuid  the unique identifier of the job
     * @param status   the current status of the job
     */
    public EdpsJobDto(String uuid, String jobUuid, String status) {
        this.uuid = uuid;
        this.jobUuid = jobUuid;
        this.state = status;
    }

    /**
     * Gets the unique identifier of the job model.
     *
     * @return the uuid
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * Sets the unique identifier of the job model.
     *
     * @param uuid the uuid to set
     */
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     * Gets the unique identifier of the job.
     *
     * @return the jobUuid
     */
    public String getJobId() {
        return jobUuid;
    }

    /**
     * Sets the unique identifier of the job.
     *
     * @param jobUuid the jobUuid to set
     */
    public void setJobUuid(String jobUuid) {
        this.jobUuid = jobUuid;
    }

    /**
     * Gets the current status of the job.
     *
     * @return the status
     */
    public String getState() {
        return state;
    }

    /**
     * Sets the current status of the job.
     *
     * @param state the status to set
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * Retrieves the details.
     *
     * @return the current details.
     */
    public String getDetails() {
        return details;
    }

    /**
     * Sets the details.
     *
     * @param details the new details to set.
     */
    public void setDetails(String details) {
        this.details = details;
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

    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }
    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public String getResultUrl() {
        return resultUrl;
    }
    public void setResultUrl(String resultUrl) {
        this.resultUrl = resultUrl;
    }
}
