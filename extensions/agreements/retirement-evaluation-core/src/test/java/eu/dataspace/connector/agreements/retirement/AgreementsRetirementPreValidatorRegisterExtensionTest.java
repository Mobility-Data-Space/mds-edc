package eu.dataspace.connector.agreements.retirement;


import org.eclipse.edc.connector.controlplane.contract.spi.policy.TransferProcessPolicyContext;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorContext;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class AgreementsRetirementPreValidatorRegisterExtensionTest {

    private final PolicyEngine policyEngine = mock();

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(PolicyEngine.class, policyEngine);
    }

    @Test
    public void verify_functionIsRegisteredOnInitialization(ServiceExtensionContext context, AgreementsRetirementPreValidatorRegisterExtension extension) {
        extension.initialize(context);

        verify(policyEngine, times(1))
                .registerPreValidator(eq(TransferProcessPolicyContext.class), any());
        verify(policyEngine, times(1))
                .registerPreValidator(eq(PolicyMonitorContext.class), any());
    }
}
