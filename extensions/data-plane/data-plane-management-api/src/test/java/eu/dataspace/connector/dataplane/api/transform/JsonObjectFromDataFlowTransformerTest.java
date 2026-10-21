package eu.dataspace.connector.dataplane.api.transform;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.TransferType;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static eu.dataspace.connector.dataplane.api.transform.JsonObjectFromDataFlowTransformer.DATA_FLOW_AGREEMENT_ID;
import static eu.dataspace.connector.dataplane.api.transform.JsonObjectFromDataFlowTransformer.DATA_FLOW_ASSET_ID;
import static eu.dataspace.connector.dataplane.api.transform.JsonObjectFromDataFlowTransformer.DATA_FLOW_PARTICIPANT_ID;
import static eu.dataspace.connector.dataplane.api.transform.JsonObjectFromDataFlowTransformer.DATA_FLOW_SOURCE;
import static eu.dataspace.connector.dataplane.api.transform.JsonObjectFromDataFlowTransformer.DATA_FLOW_STATE;
import static eu.dataspace.connector.dataplane.api.transform.JsonObjectFromDataFlowTransformer.DATA_FLOW_TRANSFER_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.STARTED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.types.domain.transfer.FlowType.PULL;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectFromDataFlowTransformerTest {

    private final JsonBuilderFactory factory = Json.createBuilderFactory(Map.of());
    private final JsonObjectFromDataFlowTransformer transformer = new JsonObjectFromDataFlowTransformer(factory);

    @Test
    void transform() {
        var context = mock(TransformerContext.class);
        var sourceJson = factory.createObjectBuilder().add("type", "HttpData").build();
        when(context.transform(isA(DataAddress.class), eq(jakarta.json.JsonObject.class))).thenReturn(sourceJson);

        var dataFlow = DataFlow.Builder.newInstance()
                .id("flow-id")
                .state(STARTED.code())
                .assetId("asset-id")
                .agreementId("agreement-id")
                .participantId("participant-id")
                .transferType(new TransferType("HttpData", PULL))
                .source(DataAddress.Builder.newInstance().type("HttpData").keyName("alias").build())
                .build();

        var result = transformer.transform(dataFlow, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(ID)).isEqualTo("flow-id");
        assertThat(result.getString(TYPE)).isEqualTo(JsonObjectFromDataFlowTransformer.DATA_FLOW_TYPE);
        assertThat(result.getString(DATA_FLOW_STATE)).isNotNull();
        assertThat(result.getString(DATA_FLOW_ASSET_ID)).isEqualTo("asset-id");
        assertThat(result.getString(DATA_FLOW_AGREEMENT_ID)).isEqualTo("agreement-id");
        assertThat(result.getString(DATA_FLOW_PARTICIPANT_ID)).isEqualTo("participant-id");
        assertThat(result.getString(DATA_FLOW_TRANSFER_TYPE)).isEqualTo("HttpData-PULL");
        assertThat(result.getJsonObject(DATA_FLOW_SOURCE)).isEqualTo(sourceJson);
        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void transform_shouldOmitNullFields() {
        var context = mock(TransformerContext.class);

        var dataFlow = DataFlow.Builder.newInstance().id("flow-id").state(STARTED.code()).build();

        var result = transformer.transform(dataFlow, context);

        assertThat(result).isNotNull();
        assertThat(result.containsKey(DATA_FLOW_ASSET_ID)).isFalse();
        assertThat(result.containsKey(DATA_FLOW_SOURCE)).isFalse();
        verify(context, never()).reportProblem(anyString());
    }
}
