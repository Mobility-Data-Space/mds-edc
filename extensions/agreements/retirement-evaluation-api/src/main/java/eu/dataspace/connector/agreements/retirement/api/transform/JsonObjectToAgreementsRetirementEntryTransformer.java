package eu.dataspace.connector.agreements.retirement.api.transform;

import eu.dataspace.connector.agreements.retirement.api.model.AgreementsRetirementEntry;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.JsonLdToModelTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static eu.dataspace.connector.agreements.retirement.api.model.AgreementsRetirementEntry.AR_ENTRY_AGREEMENT_ID;
import static eu.dataspace.connector.agreements.retirement.api.model.AgreementsRetirementEntry.AR_ENTRY_REASON;

@Deprecated(since = "2.9.0")
public class JsonObjectToAgreementsRetirementEntryTransformer extends JsonLdToModelTransformer<JsonObject, AgreementsRetirementEntry> {

    public JsonObjectToAgreementsRetirementEntryTransformer() {
        super(JsonObject.class, AgreementsRetirementEntry.class);
    }

    @Override
    public @Nullable AgreementsRetirementEntry transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var agreementId = transformString(jsonObject.get(AR_ENTRY_AGREEMENT_ID), context);
        var reason = jsonObject.get(AR_ENTRY_REASON);
        return new AgreementsRetirementEntry(agreementId, reason == null ? null : transformString(reason, context), 0L);
    }
}
