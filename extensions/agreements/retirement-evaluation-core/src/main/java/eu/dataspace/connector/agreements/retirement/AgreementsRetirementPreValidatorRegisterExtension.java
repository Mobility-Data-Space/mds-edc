package eu.dataspace.connector.agreements.retirement;

import org.eclipse.edc.connector.controlplane.contract.spi.policy.TransferProcessPolicyContext;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import eu.dataspace.connector.agreements.retirement.spi.service.AgreementsRetirementService;

import static eu.dataspace.connector.agreements.retirement.AgreementsRetirementPreValidatorRegisterExtension.NAME;


@Extension(value = NAME)
public class AgreementsRetirementPreValidatorRegisterExtension implements ServiceExtension {

    public static final String NAME = "Agreements Retirement Policy Function Extension";

    @Inject
    private AgreementsRetirementService service;

    @Inject
    private PolicyEngine policyEngine;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var validator = new AgreementRetirementValidator(service);
        policyEngine.registerPreValidator(TransferProcessPolicyContext.class, validator.transferProcess());
        policyEngine.registerPreValidator(PolicyMonitorContext.class, validator.policyMonitor());
    }
}
