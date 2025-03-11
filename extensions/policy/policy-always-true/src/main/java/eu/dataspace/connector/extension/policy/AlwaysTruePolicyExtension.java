package eu.dataspace.connector.extension.policy;

import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.services.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;

import static eu.dataspace.connector.extension.policy.AlwaysTruePolicyExtension.NAME;

/**
 * Creates an "always-true" policy with no real constraint in it
 */
@Extension(NAME)
public class AlwaysTruePolicyExtension implements ServiceExtension {

    static final String NAME = "Always True Policy";
    static final String ALWAYS_TRUE_POLICY_ID = "always-true";

    @Inject
    private PolicyDefinitionService policyDefinitionService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void prepare() {
        if (policyDefinitionService.findById(ALWAYS_TRUE_POLICY_ID) == null) {
            var policyDefinition = alwaysTruePolicy();

            policyDefinitionService.create(policyDefinition);
        }
    }

    private static PolicyDefinition alwaysTruePolicy() {
        var alwaysTruePermission = Permission.Builder.newInstance()
                .action(Action.Builder.newInstance().type("use").build())
                .build();
        var policy = Policy.Builder.newInstance()
                .permission(alwaysTruePermission)
                .build();

        return PolicyDefinition.Builder.newInstance()
                .id(ALWAYS_TRUE_POLICY_ID)
                .policy(policy)
                .build();
    }
}
