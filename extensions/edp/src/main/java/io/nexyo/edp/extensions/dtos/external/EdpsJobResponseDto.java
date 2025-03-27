package io.nexyo.edp.extensions.dtos.external;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for the response of a job.
 */
public record EdpsJobResponseDto(
        @JsonProperty("job_id")
        String jobUuid,

        @JsonProperty("state")
        String state,

        @JsonProperty("state_details")
        String details
) {
}
