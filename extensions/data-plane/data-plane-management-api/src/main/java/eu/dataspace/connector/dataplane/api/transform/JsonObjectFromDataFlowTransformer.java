package eu.dataspace.connector.dataplane.api.transform;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.TransferType;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Transforms a {@link DataFlow} into its JSON-LD representation. Source and destination addresses are serialized as
 * nested {@link DataAddress} objects, which surface vault key aliases but never resolved secrets.
 */
public class JsonObjectFromDataFlowTransformer extends AbstractJsonLdTransformer<DataFlow, JsonObject> {

    public static final String DATA_FLOW_TYPE = EDC_NAMESPACE + "DataFlow";
    public static final String DATA_FLOW_STATE = EDC_NAMESPACE + "state";
    public static final String DATA_FLOW_STATE_TIMESTAMP = EDC_NAMESPACE + "stateTimestamp";
    public static final String DATA_FLOW_CREATED_AT = EDC_NAMESPACE + "createdAt";
    public static final String DATA_FLOW_UPDATED_AT = EDC_NAMESPACE + "updatedAt";
    public static final String DATA_FLOW_ASSET_ID = EDC_NAMESPACE + "assetId";
    public static final String DATA_FLOW_AGREEMENT_ID = EDC_NAMESPACE + "agreementId";
    public static final String DATA_FLOW_PARTICIPANT_ID = EDC_NAMESPACE + "participantId";
    public static final String DATA_FLOW_TRANSFER_TYPE = EDC_NAMESPACE + "transferType";
    public static final String DATA_FLOW_RUNTIME_ID = EDC_NAMESPACE + "runtimeId";
    public static final String DATA_FLOW_ERROR_DETAIL = EDC_NAMESPACE + "errorDetail";
    public static final String DATA_FLOW_SOURCE = EDC_NAMESPACE + "source";
    public static final String DATA_FLOW_DESTINATION = EDC_NAMESPACE + "destination";

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromDataFlowTransformer(JsonBuilderFactory jsonFactory) {
        super(DataFlow.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull DataFlow dataFlow, @NotNull TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder()
                .add(ID, dataFlow.getId())
                .add(TYPE, DATA_FLOW_TYPE)
                .add(DATA_FLOW_STATE, dataFlow.stateAsString())
                .add(DATA_FLOW_STATE_TIMESTAMP, dataFlow.getStateTimestamp())
                .add(DATA_FLOW_CREATED_AT, dataFlow.getCreatedAt())
                .add(DATA_FLOW_UPDATED_AT, dataFlow.getUpdatedAt());

        addIfPresent(builder, DATA_FLOW_ASSET_ID, dataFlow.getAssetId());
        addIfPresent(builder, DATA_FLOW_AGREEMENT_ID, dataFlow.getAgreementId());
        addIfPresent(builder, DATA_FLOW_PARTICIPANT_ID, dataFlow.getParticipantId());
        addIfPresent(builder, DATA_FLOW_RUNTIME_ID, dataFlow.getRuntimeId());
        addIfPresent(builder, DATA_FLOW_ERROR_DETAIL, dataFlow.getErrorDetail());
        addIfPresent(builder, DATA_FLOW_TRANSFER_TYPE, transferTypeAsString(dataFlow.getTransferType()));

        addAddress(builder, DATA_FLOW_SOURCE, dataFlow.getSource(), context);
        addAddress(builder, DATA_FLOW_DESTINATION, dataFlow.getDestination(), context);

        return builder.build();
    }

    private void addIfPresent(JsonObjectBuilder builder, String key, String value) {
        if (value != null) {
            builder.add(key, value);
        }
    }

    private void addAddress(JsonObjectBuilder builder, String key, DataAddress address, TransformerContext context) {
        if (address != null) {
            var transformed = context.transform(address, JsonObject.class);
            if (transformed != null) {
                builder.add(key, transformed);
            }
        }
    }

    private String transferTypeAsString(TransferType transferType) {
        if (transferType == null) {
            return null;
        }
        return "%s-%s".formatted(transferType.destinationType(), transferType.flowType());
    }
}
