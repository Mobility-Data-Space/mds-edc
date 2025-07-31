package eu.dataspace.connector.validator.semantic;

import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_TYPE;

public class SemanticValidatorExtension implements ServiceExtension {

    @Inject
    private JsonObjectValidatorRegistry validator;

    @Override
    public void prepare() {
        var vocabularyProvider = new VocabularyProvider();
        var semanticValidator = SemanticValidator.instance(vocabularyProvider.provide());

        this.validator.register(EDC_ASSET_TYPE, semanticValidator);
    }

}
