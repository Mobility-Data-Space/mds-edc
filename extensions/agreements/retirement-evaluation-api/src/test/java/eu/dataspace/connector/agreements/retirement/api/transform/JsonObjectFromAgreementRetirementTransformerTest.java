package eu.dataspace.connector.agreements.retirement.api.transform;

import eu.dataspace.connector.agreements.retirement.api.model.AgreementsRetirementEntry;
import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static eu.dataspace.connector.agreements.retirement.api.model.AgreementsRetirementEntry.AR_ENTRY_AGREEMENT_ID;
import static eu.dataspace.connector.agreements.retirement.api.model.AgreementsRetirementEntry.AR_ENTRY_REASON;
import static eu.dataspace.connector.agreements.retirement.api.model.AgreementsRetirementEntry.AR_ENTRY_RETIREMENT_DATE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class JsonObjectFromAgreementRetirementTransformerTest {

    private final JsonBuilderFactory factory = Json.createBuilderFactory(Map.of());

    private final JsonObjectFromAgreementRetirementTransformer transformer = new JsonObjectFromAgreementRetirementTransformer(factory);

    @Test
    void transform() {
        var context = mock(TransformerContext.class);

        var entry = new AgreementsRetirementEntry("agreementId", "long-reason", 1234L);

        var result = transformer.transform(entry, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(AR_ENTRY_AGREEMENT_ID)).isEqualTo("agreementId");
        assertThat(result.getString(AR_ENTRY_REASON)).isEqualTo("long-reason");
        assertThat(result.getJsonNumber(AR_ENTRY_RETIREMENT_DATE).longValue()).isEqualTo(1234L);
        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void transform_shouldOmitReason_whenNull() {
        var result = transformer.transform(new AgreementsRetirementEntry("agreementId", null, 1234L), mock());

        assertThat(result).isNotNull();
        assertThat(result.containsKey(AR_ENTRY_REASON)).isFalse();
    }

}
