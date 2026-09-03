package eu.dataspace.connector.dataplane.client;

import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.dataplane.spi.DataFlowStates;
import org.eclipse.edc.signaling.domain.DataFlowPrepareMessage;
import org.eclipse.edc.signaling.domain.DataFlowResumeMessage;
import org.eclipse.edc.signaling.domain.DataFlowStatusMessage;
import org.eclipse.edc.signaling.domain.DspDataAddress;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;
import org.eclipse.edc.spi.types.domain.transfer.TransferType;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Map.entry;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static org.eclipse.edc.signaling.domain.DspDataAddress.DSP_DATA_ADDRESS_ENDPOINT;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public final class SignalingMapper {

    public static final String SIGNALING_COUNTER_PARTY_ID = EDC_NAMESPACE + "counterPartyId";
    public static final String SIGNALING_DATASPACE_CONTEXT = EDC_NAMESPACE + "dataspaceContext";

    private SignalingMapper() {
    }

    /**
     * Builds a legacy {@link DataFlowStartMessage} from the protocol {@code DataFlowStartMessage}. The protocol
     * {@code dataAddress} (the provider/source address resolved by the control plane) is mapped onto the framework
     * {@code sourceDataAddress}.
     */
    public static Result<DataFlowStartMessage> toFrameworkStartMessage(
            org.eclipse.edc.signaling.domain.DataFlowStartMessage message, AssetIndex assetIndex) {
        return transferType(message.getTransferType())
                .map(transferType -> {
                    var builder = DataFlowStartMessage.Builder.newInstance()
                            .processId(message.getProcessId())
                            .transferType(transferType)
                            .agreementId(message.getAgreementId())
                            .assetId(message.getDatasetId())
                            .participantId(message.getCounterPartyId())
                            .callbackAddress(message.getCallbackAddress());

                    ofNullable(message.getMessageId()).ifPresent(builder::id);
                    ofNullable(message.getDataAddress()).map(SignalingMapper::toDataAddress).ifPresent(builder::destinationDataAddress);
                    ofNullable(message.getDataspaceContext()).ifPresent(v -> builder.property(SIGNALING_DATASPACE_CONTEXT, v));

                    ofNullable(message.getMetadata())
                            .map(metadata -> {
                                if (metadata.isEmpty()) {
                                    // if no dataplaneMetadata was in the message, get it out of the data address
                                    return assetIndex.findById(message.getDatasetId()).getDataAddress().getProperties()
                                            .entrySet().stream()
                                            .flatMap(entry -> Stream.of(
                                                    entry,
                                                    entry(entry.getKey().replace(EDC_NAMESPACE, ""), entry.getValue()))
                                            )
                                            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
                                } else {
                                    return metadata;
                                }
                            })
                            .map(SignalingMapper::toDataAddress).ifPresent(builder::sourceDataAddress);

                    return builder.build();
                });
    }

    /**
     * Builds a legacy {@link DataFlowProvisionMessage} from the protocol {@code DataFlowPrepareMessage}.
     */
    public static Result<DataFlowProvisionMessage> toFrameworkProvisionMessage(DataFlowPrepareMessage message) {
        return transferType(message.getTransferType())
                .map(transferType -> {
                    var builder = DataFlowProvisionMessage.Builder.newInstance()
                            .processId(message.getProcessId())
                            .transferType(transferType)
                            .agreementId(message.getAgreementId())
                            .assetId(message.getDatasetId())
                            .participantId(message.getCounterPartyId())
                            .callbackAddress(message.getCallbackAddress());

                    ofNullable(message.getDataspaceContext()).ifPresent(v -> builder.property(SIGNALING_DATASPACE_CONTEXT, v));

                    return builder.build();
                });
    }

    /**
     * Builds a legacy {@link DataFlowStartMessage} for a resume request. The legacy framework has no native resume,
     * so a resume is mapped onto {@code start()} for the already-existing flow identified by {@code flowId}. Note the
     * {@code transferType} cannot be recovered from a resume message; callers relying on it must re-send it.
     */
    public static DataFlowStartMessage toFrameworkResumeMessage(String flowId, DataFlowResumeMessage message) {
        var builder = DataFlowStartMessage.Builder.newInstance()
                .processId(flowId);

        ofNullable(message.getMessageId()).ifPresent(builder::id);
        ofNullable(message.getDataAddress()).map(SignalingMapper::toDataAddress).ifPresent(builder::sourceDataAddress);

        return builder.build();
    }

    /**
     * Converts a protocol {@link DspDataAddress} to a legacy {@link DataAddress}.
     */
    public static DataAddress toDataAddress(DspDataAddress dataAddress) {
        var builder = DataAddress.Builder.newInstance()
                .type(dataAddress.getEndpointType());

        dataAddress.getEndpointProperties().forEach(property -> builder.property(property.getName(), property.getValue()));

        return builder
                .property(DSP_DATA_ADDRESS_ENDPOINT, dataAddress.getEndpoint())
                .build();
    }


    /**
     * Converts a protocol {@link DspDataAddress} to a legacy {@link DataAddress}.
     */
    public static DataAddress toDataAddress(Map<String, Object> metadata) {
        var builder = DataAddress.Builder.newInstance()
                .type(metadata.get("type").toString());

        metadata.forEach((key, value) -> builder.property(key, value.toString()));

        return builder.build();
    }

    /**
     * Converts a legacy {@link DataAddress} to a protocol {@link DspDataAddress}.
     */
    public static DspDataAddress toDspDataAddress(DataAddress dataAddress) {
        var builder = DspDataAddress.Builder.newInstance()
                .endpointType(dataAddress.getType())
                .endpoint(dataAddress.getStringProperty(DSP_DATA_ADDRESS_ENDPOINT));

        dataAddress.getProperties().forEach((key, value) -> builder.property(key, String.valueOf(value)));

        return builder.build();
    }

    /**
     * Maps a framework {@link DataFlowStates} onto the Data Plane Signaling protocol state vocabulary.
     */
    public static String toProtocolState(DataFlowStates state) {
        if (state == null) {
            return "INITIALIZED";
        }
        return switch (state) {
            case PROVISIONING, PROVISION_REQUESTED, PROVISION_NOTIFYING -> "PREPARING";
            case PROVISIONED -> "PREPARED";
            case RECEIVED -> "STARTED"; // TODO: is this correct?
            case STARTED -> "STARTED";
            case SUSPENDED -> "SUSPENDED";
            case COMPLETED, NOTIFIED -> "COMPLETED";
            case TERMINATED, FAILED, DEPROVISIONING, DEPROVISION_REQUESTED, DEPROVISIONED, DEPROVISION_FAILED ->
                    "TERMINATED";
        };
    }

    /**
     * Parses the combined Data Plane Signaling {@code transferType} string (e.g. {@code "HttpData-PULL"} or
     * {@code "HttpData-PULL-HttpData"}) into a framework {@link TransferType}.
     */
    public static Result<TransferType> transferType(String transferType) {
        if (transferType == null || transferType.isBlank()) {
            return Result.failure("transferType must not be empty");
        }
        var tokens = transferType.split("-");
        if (tokens.length < 2) {
            return Result.failure("Invalid transferType '%s': expected '<destinationType>-<PULL|PUSH>[-<responseChannel>]'".formatted(transferType));
        }
        FlowType flowType;
        try {
            flowType = FlowType.valueOf(tokens[1]);
        } catch (IllegalArgumentException e) {
            return Result.failure("Invalid flow type in transferType '%s': %s".formatted(transferType, tokens[1]));
        }
        var responseChannel = tokens.length > 2 ? tokens[2] : null;
        return Result.success(new TransferType(tokens[0], flowType, responseChannel));
    }

    /**
     * Builds a protocol {@link DataFlowStatusMessage} for the given flow, state and optional resolved data address.
     */
    public static DataFlowStatusMessage statusMessage(String dataFlowId, DataFlowStates state, DataAddress dataAddress) {
        return DataFlowStatusMessage.Builder.newInstance()
//                .dataFlowId(dataFlowId) TODO: this can be enabled when EDC updates to 1.0.0
                .state(toProtocolState(state))
                .dataAddress(Optional.ofNullable(dataAddress).map(SignalingMapper::toDspDataAddress).orElse(null))
                .build();
    }
}
