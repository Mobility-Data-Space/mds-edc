package eu.dataspace.connector.tests.extensions;

import com.fasterxml.jackson.annotation.JsonProperty;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public record KafkaEdr(
        @JsonProperty(EDC_NAMESPACE + "topic") String topic,
        @JsonProperty(EDC_NAMESPACE + "kafkaConsumerProperties") String kafkaConsumerProperties
) {
}
