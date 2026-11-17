package eu.dataspace.connector.agreements.retirement.api.transform;

import eu.dataspace.connector.agreements.retirement.api.model.AgreementsRetirementEntry;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.JsonLdFromModelTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static eu.dataspace.connector.agreements.retirement.api.model.AgreementsRetirementEntry.AR_ENTRY_AGREEMENT_ID;
import static eu.dataspace.connector.agreements.retirement.api.model.AgreementsRetirementEntry.AR_ENTRY_REASON;
import static eu.dataspace.connector.agreements.retirement.api.model.AgreementsRetirementEntry.AR_ENTRY_RETIREMENT_DATE;

@Deprecated(since = "2.9.0")
public class JsonObjectFromAgreementRetirementTransformer extends JsonLdFromModelTransformer<AgreementsRetirementEntry, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromAgreementRetirementTransformer(JsonBuilderFactory jsonFactory) {
        super(AgreementsRetirementEntry.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull AgreementsRetirementEntry entry, @NotNull TransformerContext transformerContext) {
        var builder = jsonFactory.createObjectBuilder()
                .add(AR_ENTRY_AGREEMENT_ID, entry.agreementId())
                .add(AR_ENTRY_RETIREMENT_DATE, entry.agreementRetirementDate());

        if (entry.reason() != null) {
            builder.add(AR_ENTRY_REASON, entry.reason());
        }

        return builder.build();
    }
}
