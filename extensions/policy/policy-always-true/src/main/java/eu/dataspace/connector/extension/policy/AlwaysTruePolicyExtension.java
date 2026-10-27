package eu.dataspace.connector.extension.policy;

import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.services.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtension;

import static eu.dataspace.connector.extension.policy.AlwaysTruePolicyExtension.NAME;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_SCHEMA;

/**
 * Creates an "always-true" policy with no real constraint in it
 */
@Extension(NAME)
public class AlwaysTruePolicyExtension implements ServiceExtension {

    static final String NAME = "Always True Policy";
    static final String ALWAYS_TRUE_POLICY_ID = "always-true";

    @Inject
    private PolicyDefinitionService policyDefinitionService;
    @Inject
    private SingleParticipantContextSupplier singleParticipantContextSupplier;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void start() {
        var participantContext = singleParticipantContextSupplier.get()
                .orElseThrow(f -> new EdcException("Cannot load participant context: " + f.getFailureDetail()));

        if (policyDefinitionService.findById(participantContext.getParticipantContextId(), ALWAYS_TRUE_POLICY_ID) == null) {

            var policyDefinition = PolicyDefinition.Builder.newInstance()
                    .id(ALWAYS_TRUE_POLICY_ID)
                    .participantContextId(participantContext.getParticipantContextId())
                    .policy(alwaysTruePolicy())
                    .build();

            policyDefinitionService.create(policyDefinition)
                    .orElseThrow(f -> new EdcException("Cannot create 'always-true' policy: " + f.getFailureDetail()));
        }
    }

    private Policy alwaysTruePolicy() {
        var alwaysTruePermission = Permission.Builder.newInstance()
                .action(Action.Builder.newInstance().type(ODRL_SCHEMA + "use").build())
                .build();
        return Policy.Builder.newInstance()
                .permission(alwaysTruePermission)
                .build();
    }
}
