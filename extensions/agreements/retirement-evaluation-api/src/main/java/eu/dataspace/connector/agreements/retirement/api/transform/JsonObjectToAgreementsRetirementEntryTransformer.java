package eu.dataspace.connector.agreements.retirement.api.transform;

import eu.dataspace.connector.agreements.retirement.spi.types.AgreementsRetirementEntry;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static eu.dataspace.connector.agreements.retirement.spi.types.AgreementsRetirementEntry.AR_ENTRY_AGREEMENT_ID;
import static eu.dataspace.connector.agreements.retirement.spi.types.AgreementsRetirementEntry.AR_ENTRY_REASON;
import static eu.dataspace.connector.agreements.retirement.spi.types.AgreementsRetirementEntry.DEPRECATED_AR_ENTRY_REASON;

public class JsonObjectToAgreementsRetirementEntryTransformer extends AbstractJsonLdTransformer<JsonObject, AgreementsRetirementEntry> {

    private final Monitor monitor;

    public JsonObjectToAgreementsRetirementEntryTransformer(Monitor monitor) {
        super(JsonObject.class, AgreementsRetirementEntry.class);
        this.monitor = monitor;
    }

    @Override
    public @Nullable AgreementsRetirementEntry transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var entryBuilder = AgreementsRetirementEntry.Builder.newInstance();
        entryBuilder.withAgreementId(transformString(jsonObject.get(AR_ENTRY_AGREEMENT_ID), context));
        var reason = jsonObject.get(AR_ENTRY_REASON);
        if (reason != null) {
            entryBuilder.withReason(transformString(reason, context));
        } else {
            monitor.warning("AgreementsRetirementEntry '%s' attribute has been deprecated in favor of '%s', please adapt your clients".formatted(DEPRECATED_AR_ENTRY_REASON, AR_ENTRY_REASON));
            var deprecatedReason = jsonObject.get(DEPRECATED_AR_ENTRY_REASON);
            entryBuilder.withReason(transformString(deprecatedReason, context));
        }

        return entryBuilder.build();
    }
}
