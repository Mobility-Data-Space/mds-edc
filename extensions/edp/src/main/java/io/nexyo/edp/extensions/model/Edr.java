package io.nexyo.edp.extensions.model;

/**
 * Represent an Endpoint Data Reference
 *
 * @param url the endpoint
 * @param authorization the authorization code
 */
public record Edr(String url, String authorization) {
}
