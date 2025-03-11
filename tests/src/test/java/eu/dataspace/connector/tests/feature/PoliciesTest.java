package eu.dataspace.connector.tests.feature;

import eu.dataspace.connector.tests.MdsParticipant;
import org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.TransferProcessPolicyContext;
import org.eclipse.edc.connector.controlplane.services.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_USE_ACTION_ATTRIBUTE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class PoliciesTest {

    private static final MdsParticipant RUNTIME = MdsParticipant.Builder.newInstance()
            .id("runtime").name("runtime")
            .build();

    @RegisterExtension
    private static final RuntimeExtension RUNTIME_EXTENSION = new RuntimePerClassExtension(
            new EmbeddedRuntime("runtime", ":launchers:connector-inmemory")
                    .configurationProvider(RUNTIME::getConfiguration))
            .registerSystemExtension(ServiceExtension.class, RUNTIME.seedVaultKeys());


    @Test
    void alwaysTrue_shouldProvideAlwaysTruePolicy() {
        var policyDefinitionService = RUNTIME_EXTENSION.getService(PolicyDefinitionService.class);

        var alwaysTruePolicy = policyDefinitionService.findById("always-true");

        assertThat(alwaysTruePolicy).isNotNull();
    }

    @Test
    void alwaysTrue_shouldEvaluatePolicy() {
        var policyEngine = RUNTIME_EXTENSION.getService(PolicyEngine.class);
        var policy = Policy.Builder.newInstance()
                .permission(Permission.Builder.newInstance()
                        .action(Action.Builder.newInstance().type("use").build())
                        .build()
                )
                .build();

        var evaluated = policyEngine.evaluate(policy, new CatalogPolicyContext(new ParticipantAgent(emptyMap(), emptyMap())));

        assertThat(evaluated).isSucceeded();
    }

    @ParameterizedTest
    @ArgumentsSource(ReferringConnectorContexts.class)
    void referringConnector_shouldEvaluatePolicy(Function<ParticipantAgent, PolicyContext> contextProvider) {
        var policyEngine = RUNTIME_EXTENSION.getService(PolicyEngine.class);
        var policy = Policy.Builder.newInstance()
                .permission(Permission.Builder.newInstance()
                        .action(Action.Builder.newInstance().type(ODRL_USE_ACTION_ATTRIBUTE).build())
                        .constraint(AtomicConstraint.Builder.newInstance()
                                .leftExpression(new LiteralExpression("REFERRING_CONNECTOR"))
                                .operator(Operator.EQ)
                                .rightExpression(new LiteralExpression("http://any"))
                                .build())
                        .build()
                )
                .build();
        var claims = Map.<String, Object>of("referringConnector", "http://another");

        var evaluated = policyEngine.evaluate(policy, contextProvider.apply(new ParticipantAgent(claims, emptyMap())));

        assertThat(evaluated).isFailed();
    }

    @ParameterizedTest
    @ArgumentsSource(TimeIntervalContexts.class)
    void timeInterval_shouldEvaluatePolicy(ParticipantAgentPolicyContext policyContext) {
        var policyEngine = RUNTIME_EXTENSION.getService(PolicyEngine.class);
        var policy = Policy.Builder.newInstance()
                .permission(Permission.Builder.newInstance()
                        .action(Action.Builder.newInstance().type(ODRL_USE_ACTION_ATTRIBUTE).build())
                        .constraint(AtomicConstraint.Builder.newInstance()
                                .leftExpression(new LiteralExpression("POLICY_EVALUATION_TIME"))
                                .operator(Operator.LT)
                                .rightExpression(new LiteralExpression(OffsetDateTime.now().minusDays(1).toString()))
                                .build())
                        .build()
                )
                .build();

        var evaluated = policyEngine.evaluate(policy, policyContext);

        assertThat(evaluated).isFailed();
    }

    private static class ReferringConnectorContexts implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            Function<ParticipantAgent, CatalogPolicyContext> catalog = CatalogPolicyContext::new;
            Function<ParticipantAgent, ContractNegotiationPolicyContext> contractNegotiation = ContractNegotiationPolicyContext::new;
            return Stream.of(arguments(catalog), arguments(contractNegotiation));
        }
    }

    private static class TimeIntervalContexts implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            var participantAgent = new ParticipantAgent(emptyMap(), emptyMap());
            return Stream.of(
                    arguments(new CatalogPolicyContext(participantAgent)),
                    arguments(new ContractNegotiationPolicyContext(participantAgent)),
                    arguments(new TransferProcessPolicyContext(participantAgent, null, Instant.now()))
            );
        }
    }
}
