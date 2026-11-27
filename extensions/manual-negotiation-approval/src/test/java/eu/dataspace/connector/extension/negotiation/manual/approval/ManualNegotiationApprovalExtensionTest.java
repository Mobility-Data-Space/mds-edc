package eu.dataspace.connector.extension.negotiation.manual.approval;

import eu.dataspace.connector.extension.negotiation.manual.approval.logic.ManualApprovalPolicyFunction;
import eu.dataspace.connector.extension.negotiation.manual.approval.logic.ManualApprovalValidator;
import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.ApprovalContractNegotiationPolicyContext;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.junit.extensions.TestExtensionContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static eu.dataspace.connector.extension.negotiation.manual.approval.ManualNegotiationApprovalExtension.APPROVAL_ENABLED_SETTING;
import static eu.dataspace.connector.extension.negotiation.manual.approval.logic.ManualApprovalPolicyFunction.MANUAL_APPROVAL_CONSTRAINT_KEY;
import static org.eclipse.edc.connector.controlplane.contract.spi.policy.ApprovalContractNegotiationPolicyContext.APPROVAL_SCOPE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class ManualNegotiationApprovalExtensionTest {

    private final PolicyEngine policyEngine = mock();
    private final RuleBindingRegistry ruleBindingRegistry = mock();
    private final Monitor monitor = mock();

    @BeforeEach
    void setUp(TestExtensionContext context) {
        context.registerService(PolicyEngine.class, policyEngine);
        context.registerService(RuleBindingRegistry.class, ruleBindingRegistry);
        context.registerService(Monitor.class, monitor);
    }

    @Test
    void shouldRegisterPreValidatorOnApprovalScope(TestExtensionContext context, ObjectFactory objectFactory) {
        context.setConfig(ConfigFactory.fromMap(Map.of(APPROVAL_ENABLED_SETTING, "true")));

        objectFactory.constructInstance(ManualNegotiationApprovalExtension.class).initialize(context);

        verify(policyEngine).registerPreValidator(eq(ApprovalContractNegotiationPolicyContext.class), isA(ManualApprovalValidator.class));
        verify(monitor, never()).warning(anyString());
    }

    @Test
    void shouldRegisterManualApprovalFunctionOnApprovalScopeOnly(TestExtensionContext context, ObjectFactory objectFactory) {
        context.setConfig(ConfigFactory.fromMap(Map.of(APPROVAL_ENABLED_SETTING, "true")));

        objectFactory.constructInstance(ManualNegotiationApprovalExtension.class).initialize(context);

        verify(ruleBindingRegistry).bind(MANUAL_APPROVAL_CONSTRAINT_KEY, APPROVAL_SCOPE);
        verify(ruleBindingRegistry).bind(anyString(), anyString());
        verify(policyEngine).registerFunction(eq(ApprovalContractNegotiationPolicyContext.class), eq(Permission.class),
                eq(MANUAL_APPROVAL_CONSTRAINT_KEY), isA(ManualApprovalPolicyFunction.class));
    }

    @Test
    void shouldWarn_whenEdcApprovalIsNotEnabled(TestExtensionContext context, ObjectFactory objectFactory) {
        context.setConfig(ConfigFactory.empty());

        objectFactory.constructInstance(ManualNegotiationApprovalExtension.class).initialize(context);

        verify(policyEngine).registerPreValidator(eq(ApprovalContractNegotiationPolicyContext.class), isA(ManualApprovalValidator.class));
        verify(monitor).warning(anyString());
    }
}
