package eu.dataspace.connector.tests.extensions;

import com.fasterxml.jackson.annotation.JsonProperty;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public record KafkaEdr(
        @JsonProperty(EDC_NAMESPACE + "topic") String topic,
        @JsonProperty(EDC_NAMESPACE + "clientId") String clientId,
        @JsonProperty(EDC_NAMESPACE + "clientSecret") String clientSecret,
        @JsonProperty(EDC_NAMESPACE + "tokenEndpoint") String tokenEndpoint,
        @JsonProperty(EDC_NAMESPACE + "kafka.group.prefix") String kafkaGroupPrefix,
        @JsonProperty(EDC_NAMESPACE + "kafka.security.protocol") String kafkaSecurityProtocol,
        @JsonProperty(EDC_NAMESPACE + "kafka.sasl.mechanism") String kafkaSaslMechanism,
        @JsonProperty(EDC_NAMESPACE + "kafka.bootstrap.servers") String kafkaBootstrapServers
) {
}
