package io.nexyo.edp.extensions.dtos.internal;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for the response of a generic request.
 */
public record GenericResponseDto(
        @JsonProperty("message") String message,
        @JsonProperty("status") Status status) {
}
