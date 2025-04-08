package io.nexyo.edp.extensions.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * Utility class for creating mock data.
 */
public class MockUtils {

    /**
     * Private constructor to prevent instantiation.
     */
    private MockUtils() {
    }


    /**
     * Creates a mock request body for creating an EDPS job.
     *
     *
     * @param assetId The unique identifier of the asset to create a job for
     * @return The request body for creating an EDPS job
     */
    public static Map<String, Object> createRequestBody(String assetId) {
        Map<String, Object> innerRequestBody = new HashMap<>();
        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy-HH:mm:ss"));
        innerRequestBody.put("name", "Example EDC Asset - " + timestamp);

        // Create assetRef object
        Map<String, Object> assetRef = new HashMap<>();
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6);
        assetRef.put("assetId", assetId + "-" + randomSuffix);
        assetRef.put("assetUrl", "https://example.com/data");
        assetRef.put("assetVersion", "1.0");

        Map<String, String> dataSpace = Map.of("name", "Example Dataspace", "url", "https://dataspace.com");
        assetRef.put("dataSpace", dataSpace);

        Map<String, String> publisher = Map.of("name", "Publisher Name", "url", "https://publisher.com");
        assetRef.put("publisher", publisher);

        assetRef.put("publishDate", "2025-01-22T16:25:09.719Z");

        Map<String, String> license = Map.of("name", "License Name", "url", "https://license.com");
        assetRef.put("license", license);

        // Add assetRef to assetRefs array
        List<Map<String, Object>> assetRefs = new ArrayList<>();
        assetRefs.add(assetRef);
        innerRequestBody.put("assetRefs", assetRefs);

        // Add remaining fields at the root level
        innerRequestBody.put("dataCategory", "Example Category");
        innerRequestBody.put("assetProcessingStatus", "Original Data");
        innerRequestBody.put("description", "Example Description");
        innerRequestBody.put("tags", List.of("tag1", "tag2"));
        innerRequestBody.put("dataSubCategory", "SubCategory");
        innerRequestBody.put("assetTypeInfo", "string");
        innerRequestBody.put("transferTypeFlag", "static");
        innerRequestBody.put("immutabilityFlag", "immutable");
        innerRequestBody.put("growthFlag", "Bytes/day");
        innerRequestBody.put("transferTypeFrequency", "updates by second");
        innerRequestBody.put("nda", "NDA text");
        innerRequestBody.put("dpa", "DPA text");
        innerRequestBody.put("dataLog", "Data Log Entry");
        innerRequestBody.put("freely_available", true);

        // Wrap the inner object in an outer object with the key "user_provided_edp_data"
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("user_provided_edp_data", innerRequestBody);

        return requestBody;
    }
}
