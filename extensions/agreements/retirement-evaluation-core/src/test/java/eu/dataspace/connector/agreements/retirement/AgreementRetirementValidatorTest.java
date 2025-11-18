package eu.dataspace.connector.agreements.retirement;


import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Policy;
import eu.dataspace.connector.agreements.retirement.spi.service.AgreementsRetirementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgreementRetirementValidatorTest {

    private AgreementRetirementValidator validator;
    private final AgreementsRetirementService service = mock();
    private final PolicyContext context = mock();

    @BeforeEach
    public void setup() {
        validator = new AgreementRetirementValidator(service);
    }

    @Test
    @DisplayName("Verify validator returns true if no agreement is found in policyContext")
    public void verify_agreementExistsInPolicyContext() {
        assertThat(validator.validate(null, context)).isTrue();
    }

    @Test
    public void verify_returnFalseWhenRetired() {
        var agreementId = "test-agreement";
        var agreement = buildAgreement(agreementId);

        when(service.isRetired(agreementId))
                .thenReturn(true);

        var result = validator.validate(agreement, context);

        assertThat(result).isFalse();
        verify(context, times(1)).reportProblem(anyString());
    }

    @Test
    public void verify_returnFalseWhenNotRetired() {
        var agreementId = "test-agreement";
        var agreement = buildAgreement(agreementId);

        when(service.isRetired(agreementId))
                .thenReturn(false);

        var result = validator.validate(agreement, context);

        assertThat(result).isTrue();
        verify(context, never()).reportProblem(anyString());
    }

    private ContractAgreement buildAgreement(String agreementId) {
        return ContractAgreement.Builder.newInstance()
                .id(agreementId)
                .assetId("fake")
                .consumerId("fake")
                .providerId("fake")
                .policy(mock(Policy.class))
                .build();
    }

}
