package eu.dataspace.connector.tests.extensions;

import com.fasterxml.jackson.annotation.JsonProperty;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public record KafkaEdr(
        @JsonProperty(EDC_NAMESPACE + "id") String id,
        @JsonProperty(EDC_NAMESPACE + "contractId") String contractId,
        @JsonProperty(EDC_NAMESPACE + "endpoint") String endpoint,
        @JsonProperty(EDC_NAMESPACE + "topic") String topic,
        @JsonProperty(EDC_NAMESPACE + "authKey") String authKey,
        @JsonProperty(EDC_NAMESPACE + "authCode") String authCode,
        @JsonProperty(EDC_NAMESPACE + "authorization") String token,
        @JsonProperty(EDC_NAMESPACE + "kafka.poll.duration") String kafkaPollDuration,
        @JsonProperty(EDC_NAMESPACE + "kafka.group.prefix") String kafkaGroupPrefix,
        @JsonProperty(EDC_NAMESPACE + "kafka.security.protocol") String kafkaSecurityProtocol,
        @JsonProperty(EDC_NAMESPACE + "kafka.sasl.mechanism") String kafkaSaslMechanism,
        @JsonProperty(EDC_NAMESPACE + "kafka.bootstrap.servers") String kafkaBootstrapServers
) {
}
