package eu.dataspace.connector.extension.negotiation.manual.approval.logic;

import org.eclipse.edc.connector.controlplane.contract.spi.policy.ApprovalContractNegotiationPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.result.Result;

/**
 * Evaluates the MDS {@code ManualApproval} constraint, as specified by the MDS ODRL profile 2026/1: the provider approves
 * the contract negotiation by hand.
 * <p>
 * Only the {@code eq} operator with the right operand {@code true} is allowed.
 * <p>
 * The function is bound to the {@link ApprovalContractNegotiationPolicyContext#APPROVAL_SCOPE} only, where it is never
 * satisfied: the EDC approval guard then holds the provider negotiation in {@code REQUESTED} state until it gets manually
 * approved or rejected. In any other scope the constraint is not bound, so it is filtered out of the evaluation, while it
 * is still part of the offer published in the catalog.
 */
public class ManualApprovalPolicyFunction implements AtomicConstraintRuleFunction<Permission, ApprovalContractNegotiationPolicyContext> {

    public static final String MANUAL_APPROVAL_CONSTRAINT_KEY = "https://w3id.org/mobility-dataspace/2026/1/policy/ManualApproval";

    private static final String PROBLEM_PREFIX = "Failing evaluation of ManualApproval constraint. ";

    @Override
    public boolean evaluate(Operator operator, Object rightValue, Permission rule, ApprovalContractNegotiationPolicyContext context) {
        var validation = validate(operator, rightValue, rule);
        if (validation.failed()) {
            context.reportProblem(PROBLEM_PREFIX + validation.getFailureDetail());
        } else {
            context.reportProblem("Contract policy requires manual approval");
        }
        return false;
    }

    @Override
    public Result<Void> validate(Operator operator, Object rightValue, Permission rule) {
        if (operator != Operator.EQ) {
            return Result.failure("Unsupported operator: '%s', only 'eq' is allowed".formatted(operator.getOdrlRepresentation()));
        }
        if (!"true".equals(rightValue)) {
            return Result.failure("Right operand must be the string 'true', but was '%s'".formatted(rightValue));
        }
        return Result.success();
    }

    @Override
    public String name() {
        return "MDS ManualApproval";
    }
}
