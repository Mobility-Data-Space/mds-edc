package eu.dataspace.connector.agreements.retirement;

import org.eclipse.edc.connector.controlplane.contract.spi.policy.TransferProcessPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorContext;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyValidatorRule;
import eu.dataspace.connector.agreements.retirement.spi.service.AgreementsRetirementService;


public record AgreementRetirementValidator(AgreementsRetirementService agreementsRetirementService) {

    public PolicyValidatorRule<TransferProcessPolicyContext> transferProcess() {
        return (policy, context) -> validate(context.contractAgreement(), context);
    }

    public PolicyValidatorRule<PolicyMonitorContext> policyMonitor() {
        return (policy, context) -> validate(context.contractAgreement(), context);
    }

    public Boolean validate(ContractAgreement agreement, PolicyContext policyContext) {
        if (agreement != null) {
            if (agreementsRetirementService.isRetired(agreement.getId())) {
                policyContext.reportProblem(String.format("Contract Agreement with ID=%s has been retired", agreement.getId()));
                return false;
            }
        }
        return true;
    }
}
