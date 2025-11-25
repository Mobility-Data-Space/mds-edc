package eu.dataspace.connector.agreements.retirement.api.transform;

import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import eu.dataspace.connector.agreements.retirement.spi.types.AgreementsRetirementEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static eu.dataspace.connector.agreements.retirement.spi.types.AgreementsRetirementEntry.AR_ENTRY_AGREEMENT_ID;
import static eu.dataspace.connector.agreements.retirement.spi.types.AgreementsRetirementEntry.AR_ENTRY_REASON;

public class JsonObjectToAgreementsRetirementEntryTransformer extends AbstractJsonLdTransformer<JsonObject, AgreementsRetirementEntry> {

    public JsonObjectToAgreementsRetirementEntryTransformer() {
        super(JsonObject.class, AgreementsRetirementEntry.class);
    }

    @Override
    public @Nullable AgreementsRetirementEntry transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var entryBuilder = AgreementsRetirementEntry.Builder.newInstance();
        entryBuilder.withAgreementId(transformString(jsonObject.get(AR_ENTRY_AGREEMENT_ID), context));
        entryBuilder.withReason(transformString(jsonObject.get(AR_ENTRY_REASON), context));
        return entryBuilder.build();
    }
}
