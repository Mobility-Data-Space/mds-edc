package eu.dataspace.connector.tests.feature;

import eu.dataspace.connector.tests.MdsParticipant;
import org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.TransferProcessPolicyContext;
import org.eclipse.edc.connector.controlplane.services.spi.policydefinition.PolicyDefinitionService;
import jakarta.json.Json;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static io.restassured.http.ContentType.JSON;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_USE_ACTION_ATTRIBUTE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class PoliciesTest {

    private static final String MDS_MEMBERSHIP = "https://w3id.org/mobility-dataspace/2026/1/policy/Membership";

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

        var alwaysTruePolicy = policyDefinitionService.findById(RUNTIME.getId(), "always-true");

        assertThat(alwaysTruePolicy).isNotNull();
    }

    @Test
    void alwaysTrue_shouldEvaluatePolicy() {
        var policyEngine = RUNTIME_EXTENSION.getService(PolicyEngine.class);
        var policy = Policy.Builder.newInstance()
                .permission(Permission.Builder.newInstance()
                        .action(Action.Builder.newInstance().type(ODRL_USE_ACTION_ATTRIBUTE).build())
                        .build()
                )
                .build();

        var evaluated = policyEngine.evaluate(policy, new CatalogPolicyContext(new ParticipantAgent("any", emptyMap(), emptyMap())));

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
                                .leftExpression(new LiteralExpression(EDC_NAMESPACE + "REFERRING_CONNECTOR"))
                                .operator(Operator.EQ)
                                .rightExpression(new LiteralExpression("http://any"))
                                .build())
                        .build()
                )
                .build();
        var claims = Map.<String, Object>of("referringConnector", "http://another");

        var evaluated = policyEngine.evaluate(policy, contextProvider.apply(new ParticipantAgent("any", claims, emptyMap())));

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
                                .leftExpression(new LiteralExpression(EDC_NAMESPACE + "POLICY_EVALUATION_TIME"))
                                .operator(Operator.LT)
                                .rightExpression(new LiteralExpression(OffsetDateTime.now().minusDays(1).toString()))
                                .build())
                        .build()
                )
                .build();

        var evaluated = policyEngine.evaluate(policy, policyContext);

        assertThat(evaluated).isFailed();
    }

    @ParameterizedTest
    @ArgumentsSource(ParticipantAgentContexts.class)
    void membership_shouldEvaluatePolicy(Function<ParticipantAgent, PolicyContext> contextProvider) {
        var policyEngine = RUNTIME_EXTENSION.getService(PolicyEngine.class);
        var policy = membershipPolicy();
        var membershipCredential = VerifiableCredential.Builder.newInstance()
                .contexts(List.of("https://www.w3.org/ns/credentials/v2", "https://w3id.org/mobility-dataspace/2026/1/credentials/context.jsonld"))
                .types(List.of("VerifiableCredential", "MembershipCredential"))
                .issuer(new Issuer("did:web:issuer"))
                .issuanceDate(Instant.now())
                .credentialSubject(CredentialSubject.Builder.newInstance().id("did:web:consumer").claim("holderIdentifier", "consumer").build())
                .build();
        var member = new ParticipantAgent("did:web:consumer", Map.of("vc", List.of(membershipCredential)), emptyMap());
        var nonMember = new ParticipantAgent("did:web:consumer", emptyMap(), emptyMap());

        assertThat(policyEngine.evaluate(policy, contextProvider.apply(member))).isSucceeded();
        assertThat(policyEngine.evaluate(policy, contextProvider.apply(nonMember))).isFailed();
    }

    @Test
    void membership_shouldExpandLeftOperandWithMdsPolicyContext() {
        var policyDefinition = Json.createObjectBuilder()
                .add("@context", Json.createArrayBuilder()
                        .add("https://w3id.org/edc/connector/management/v2")
                        .add("https://w3id.org/mobility-dataspace/connector/management/v1")
                        .add("https://w3id.org/mobility-dataspace/2026/1/policy/context.jsonld"))
                .add("@type", "PolicyDefinition")
                .add("@id", "mds-members-only")
                .add("policy", Json.createObjectBuilder()
                        .add("@type", "Set")
                        .add("permission", Json.createArrayBuilder().add(Json.createObjectBuilder()
                                .add("action", "use")
                                .add("constraint", Json.createArrayBuilder().add(Json.createObjectBuilder()
                                        .add("leftOperand", "Membership")
                                        .add("operator", "eq")
                                        .add("rightOperand", "active"))))))
                .build();

        RUNTIME.baseManagementRequest()
                .contentType(JSON)
                .body(policyDefinition.toString())
                .post("/policydefinitions")
                .then()
                .log().ifValidationFails()
                .statusCode(200);

        var stored = RUNTIME_EXTENSION.getService(PolicyDefinitionService.class).findById(RUNTIME.getId(), "mds-members-only");
        assertThat(stored).isNotNull();
        var constraint = (AtomicConstraint) stored.getPolicy().getPermissions().get(0).getConstraints().get(0);
        assertThat(constraint.getLeftExpression()).isEqualTo(new LiteralExpression(MDS_MEMBERSHIP));
    }

    private Policy membershipPolicy() {
        return Policy.Builder.newInstance()
                .permission(Permission.Builder.newInstance()
                        .action(Action.Builder.newInstance().type(ODRL_USE_ACTION_ATTRIBUTE).build())
                        .constraint(AtomicConstraint.Builder.newInstance()
                                .leftExpression(new LiteralExpression(MDS_MEMBERSHIP))
                                .operator(Operator.EQ)
                                .rightExpression(new LiteralExpression("active"))
                                .build())
                        .build()
                )
                .build();
    }

    private static class ParticipantAgentContexts implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            Function<ParticipantAgent, CatalogPolicyContext> catalog = CatalogPolicyContext::new;
            Function<ParticipantAgent, ContractNegotiationPolicyContext> contractNegotiation = ContractNegotiationPolicyContext::new;
            Function<ParticipantAgent, TransferProcessPolicyContext> transferProcess = agent -> new TransferProcessPolicyContext(agent, null, Instant.now());
            return Stream.of(arguments(catalog), arguments(contractNegotiation), arguments(transferProcess));
        }
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
            var participantAgent = new ParticipantAgent("any", emptyMap(), emptyMap());
            return Stream.of(
                    arguments(new CatalogPolicyContext(participantAgent)),
                    arguments(new ContractNegotiationPolicyContext(participantAgent)),
                    arguments(new TransferProcessPolicyContext(participantAgent, null, Instant.now()))
            );
        }
    }
}
