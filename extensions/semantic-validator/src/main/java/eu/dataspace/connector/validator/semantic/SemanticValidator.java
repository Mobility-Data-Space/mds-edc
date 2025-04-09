package eu.dataspace.connector.validator.semantic;

import jakarta.json.JsonObject;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.HasSubject;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.util.List;
import java.util.Objects;

import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_PROPERTIES;
import static org.eclipse.edc.validator.spi.Violation.violation;

public class SemanticValidator implements Validator<JsonObject> {

    private final JsonLdPath path;
    private final List<String> axioms;

    public static JsonObjectValidator instance() {
        return JsonObjectValidator.newValidator()
                .verifyObject(EDC_ASSET_PROPERTIES, builder -> builder.verify(SemanticValidator::new))
                .build();
    }

    public SemanticValidator(JsonLdPath path) {
        this.path = path;
        try {
            var ontologyFile = getClass().getClassLoader().getResource("MDS-Ontology.ttl");
            var ontology = OWLManager.createOWLOntologyManager().loadOntology(IRI.create(ontologyFile));
            axioms = ontology.axioms()
                    .map(owlAxiom -> owlAxiom instanceof HasSubject<?> subject && subject.getSubject() instanceof IRI iri
                            ? iri
                            : null
                    )
                    .filter(Objects::nonNull)
                    .map(IRI::getIRIString).toList();
        } catch (OWLOntologyCreationException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public ValidationResult validate(JsonObject jsonObject) {
        var violations = jsonObject.asJsonObject().keySet().stream().filter(key -> !axioms.contains(key))
                .map(key -> violation("Asset property '%s' is not part of the MDS ontology".formatted(key), path.append(key).toString()))
                .toList();

        if (violations.isEmpty()) {
            return ValidationResult.success();
        }

        return ValidationResult.failure(violations);
    }

}
