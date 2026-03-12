package eu.dataspace.connector.agreements.retirement.api.transform;

import eu.dataspace.connector.agreements.retirement.spi.types.EnhancedContractAgreement;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public class JsonObjectFromContractAgreementEnrichedTransformer extends AbstractJsonLdTransformer<EnhancedContractAgreement, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromContractAgreementEnrichedTransformer(JsonBuilderFactory jsonFactory) {
        super(EnhancedContractAgreement.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull EnhancedContractAgreement entry, @NotNull TransformerContext transformerContext) {
        var agreement = transformerContext.transform(entry.agreement(), JsonObject.class);
        var builder = jsonFactory.createObjectBuilder(agreement);
        if (entry.retirement() != null) {
            builder.add(EDC_NAMESPACE + "isRetired", true);
            builder.add(EDC_NAMESPACE + "retiredAt", entry.retirement().getAgreementRetirementDate());
            builder.add(EDC_NAMESPACE + "retirementReason", entry.retirement().getReason());
        } else {
            builder.add(EDC_NAMESPACE + "isRetired", false);
        }

        return builder.build();

    }
}
