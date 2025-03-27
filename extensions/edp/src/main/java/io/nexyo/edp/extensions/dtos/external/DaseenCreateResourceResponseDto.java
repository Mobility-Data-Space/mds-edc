package io.nexyo.edp.extensions.dtos.external;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DaseenCreateResourceResponseDto
 */
public record DaseenCreateResourceResponseDto(
        @JsonProperty("id")
        String id,

        @JsonProperty("state")
        String state,

        @JsonProperty("message")
        String message
) {
}
