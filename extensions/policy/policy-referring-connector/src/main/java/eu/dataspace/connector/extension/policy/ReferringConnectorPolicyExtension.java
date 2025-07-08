package eu.dataspace.connector.extension.policy;

import org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static eu.dataspace.connector.extension.policy.ReferringConnectorPolicyExtension.NAME;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_USE_ACTION_ATTRIBUTE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;


@Extension(NAME)
public class ReferringConnectorPolicyExtension implements ServiceExtension {

    static final String NAME = "Referring Connector Policy";
    /**
     * The key for referring connector constraints.
     * Must be used as left operand when declaring constraints.
     * rightOperand can be a string-URL or a comma separated list of string-URLs.
     * Also supports the IN Operator with a list of string-URLs as right operand.
     *
     * <p>Example:
     *
     * <pre>
     * {
     *     "constraint": {
     *         "leftOperand": "REFERRING_CONNECTOR",
     *         "operator": "EQ",
     *         "rightOperand": "http://example.org,http://example.org"
     *     }
     * }
     * </pre>
     *
     * Constraint:
     * <pre>
     *       {
     *         "edctype": "AtomicConstraint",
     *         "leftExpression": {
     *           "edctype": "dataspaceconnector:literalexpression",
     *           "value": "REFERRING_CONNECTOR"
     *         },
     *         "rightExpression": {
     *           "edctype": "dataspaceconnector:literalexpression",
     *           "value": "http://example.org"
     *         },
     *         "operator": "EQ"
     *       }
     * </pre>
     */
    static final String REFERRING_CONNECTOR_CONSTRAINT_KEY = EDC_NAMESPACE + "REFERRING_CONNECTOR";

    @Inject
    private RuleBindingRegistry ruleBindingRegistry;

    @Inject
    private PolicyEngine policyEngine;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        registerFunctionAndBindTo(CatalogPolicyContext.class, CatalogPolicyContext.CATALOG_SCOPE);
        registerFunctionAndBindTo(ContractNegotiationPolicyContext.class, ContractNegotiationPolicyContext.NEGOTIATION_SCOPE);
    }

    private <C extends ParticipantAgentPolicyContext> void registerFunctionAndBindTo(Class<C> contextClass, String scope) {
        ruleBindingRegistry.bind(ODRL_USE_ACTION_ATTRIBUTE, scope);
        ruleBindingRegistry.bind(REFERRING_CONNECTOR_CONSTRAINT_KEY, scope);
        policyEngine.registerFunction(contextClass, Duty.class, REFERRING_CONNECTOR_CONSTRAINT_KEY, new ReferringConnectorPolicyFunction<>());
        policyEngine.registerFunction(contextClass, Permission.class, REFERRING_CONNECTOR_CONSTRAINT_KEY, new ReferringConnectorPolicyFunction<>());
        policyEngine.registerFunction(contextClass, Prohibition.class, REFERRING_CONNECTOR_CONSTRAINT_KEY, new ReferringConnectorPolicyFunction<>());
    }

}
