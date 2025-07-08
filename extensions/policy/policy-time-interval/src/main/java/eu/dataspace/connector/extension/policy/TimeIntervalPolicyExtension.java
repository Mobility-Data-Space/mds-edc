package eu.dataspace.connector.extension.policy;

import org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.TransferProcessPolicyContext;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.time.OffsetDateTime;

import static eu.dataspace.connector.extension.policy.TimeIntervalPolicyExtension.NAME;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_USE_ACTION_ATTRIBUTE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;


@Extension(NAME)
public class TimeIntervalPolicyExtension implements ServiceExtension {

    static final String NAME = "Time Interval Policy";
    static final String POLICY_EVALUATION_TIME_CONSTRAINT_KEY = EDC_NAMESPACE + "POLICY_EVALUATION_TIME";

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
        registerFunctionAndBindTo(TransferProcessPolicyContext.class, TransferProcessPolicyContext.TRANSFER_SCOPE);
    }

    private <C extends ParticipantAgentPolicyContext> void registerFunctionAndBindTo(Class<C> contextClass, String scope) {
        ruleBindingRegistry.bind(ODRL_USE_ACTION_ATTRIBUTE, scope);
        ruleBindingRegistry.bind(POLICY_EVALUATION_TIME_CONSTRAINT_KEY, scope);
        policyEngine.registerFunction(contextClass, Permission.class, POLICY_EVALUATION_TIME_CONSTRAINT_KEY, new TimeIntervalPolicyFunction<>(OffsetDateTime::now));
    }

}
