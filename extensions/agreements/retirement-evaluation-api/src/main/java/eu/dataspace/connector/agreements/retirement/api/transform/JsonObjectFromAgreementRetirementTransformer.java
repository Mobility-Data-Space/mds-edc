package eu.dataspace.connector.agreements.retirement.api.transform;

import eu.dataspace.connector.agreements.retirement.spi.types.AgreementsRetirementEntry;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static eu.dataspace.connector.agreements.retirement.spi.types.AgreementsRetirementEntry.AR_ENTRY_AGREEMENT_ID;
import static eu.dataspace.connector.agreements.retirement.spi.types.AgreementsRetirementEntry.AR_ENTRY_REASON;
import static eu.dataspace.connector.agreements.retirement.spi.types.AgreementsRetirementEntry.AR_ENTRY_RETIREMENT_DATE;
import static eu.dataspace.connector.agreements.retirement.spi.types.AgreementsRetirementEntry.DEPRECATED_AR_ENTRY_REASON;
import static eu.dataspace.connector.agreements.retirement.spi.types.AgreementsRetirementEntry.DEPRECATED_AR_ENTRY_RETIREMENT_DATE;

public class JsonObjectFromAgreementRetirementTransformer extends AbstractJsonLdTransformer<AgreementsRetirementEntry, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromAgreementRetirementTransformer(JsonBuilderFactory jsonFactory) {
        super(AgreementsRetirementEntry.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull AgreementsRetirementEntry entry, @NotNull TransformerContext transformerContext) {
        return jsonFactory.createObjectBuilder()
                .add(AR_ENTRY_AGREEMENT_ID, entry.getAgreementId())
                .add(AR_ENTRY_REASON, entry.getReason())
                .add(AR_ENTRY_RETIREMENT_DATE, entry.getAgreementRetirementDate())
                .add(DEPRECATED_AR_ENTRY_REASON, entry.getReason())
                .add(DEPRECATED_AR_ENTRY_RETIREMENT_DATE, entry.getAgreementRetirementDate())
                .build();

    }
}
