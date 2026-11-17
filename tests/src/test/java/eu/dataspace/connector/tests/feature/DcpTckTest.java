package eu.dataspace.connector.tests.feature;

import eu.dataspace.connector.tests.MdsParticipant;
import eu.dataspace.connector.tests.MdsParticipantFactory;
import eu.dataspace.connector.tests.Wallet;
import eu.dataspace.connector.tests.extensions.PostgresqlExtension;
import eu.dataspace.connector.tests.extensions.VaultExtension;
import eu.dataspace.connector.tests.tags.TckTest;
import org.eclipse.dataspacetck.core.system.ConsoleMonitor;
import org.eclipse.dataspacetck.dcp.system.DcpSystemLauncher;
import org.eclipse.dataspacetck.runtime.TckRuntime;
import org.eclipse.edc.junit.utils.LazySupplier;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspacetck.core.api.system.SystemsConstants.TCK_LAUNCHER;
import static org.eclipse.edc.util.io.Ports.getFreePort;

/**
 * Runs the DCP TCK against mds-edc, on PostgreSQL and HashiCorp Vault:
 * <ul>
 *     <li>verifier: the DCP connector (launcher {@code connector-vault-postgresql-dcp}), with the wallet as STS and DID host</li>
 *     <li>credential service (presentation and issuance): the wallet (launcher {@code wallet})</li>
 * </ul>
 */
@TckTest
public class DcpTckTest {

    private static final String VERIFIER = "verifier";
    private static final String HOLDER = "holder";
    private static final int CALLBACK_PORT = getFreePort();
    private static final String TCK_ISSUER_DID = "did:web:localhost%3A" + CALLBACK_PORT + ":issuer";

    @RegisterExtension
    @Order(0)
    static final VaultExtension VAULT = new VaultExtension();

    @RegisterExtension
    @Order(0)
    static final PostgresqlExtension POSTGRES = new PostgresqlExtension("wallet", VERIFIER);

    @RegisterExtension
    @Order(1)
    static final Wallet WALLET = MdsParticipantFactory.wallet(POSTGRES, VAULT, VERIFIER, HOLDER);

    @RegisterExtension
    @Order(2)
    static final MdsParticipant CONNECTOR = MdsParticipantFactory.hashicorpVaultDcp(VERIFIER, VAULT, POSTGRES, WALLET, new LazySupplier<>(() -> TCK_ISSUER_DID));


    @Timeout(300)
    @Test
    void verifier() {
        runTck("org.eclipse.dataspacetck.dcp.verification.presentation.verifier", Map.of(
                "dataspacetck.did.verifier", CONNECTOR.getId(),
                // the DSP catalog request triggers the verifier to resolve the holder's presentation
                "dataspacetck.vpp.trigger.endpoint", CONNECTOR.getProtocolUrl() + "/catalog/request"
        ));
    }

    @Timeout(300)
    @Test
    void presentation() {
        runTck("org.eclipse.dataspacetck.dcp.verification.presentation.cs", credentialServiceProperties());
    }

    @Timeout(300)
    @Test
    void issuance() {
        runTck("org.eclipse.dataspacetck.dcp.verification.issuance.cs", credentialServiceProperties());
    }

    private void runTck(String testPackage, Map<String, String> properties) {
        var allProperties = new HashMap<>(properties);
        allProperties.put(TCK_LAUNCHER, DcpSystemLauncher.class.getName());
        allProperties.put("dataspacetck.callback.address", "http://localhost:" + CALLBACK_PORT);
        allProperties.put("dataspacetck.port", String.valueOf(CALLBACK_PORT));

        var result = TckRuntime.Builder.newInstance()
                .properties(allProperties)
                .addPackage(testPackage)
                .monitor(new ConsoleMonitor(true, true))
                .build()
                .execute();

        assertThat(result.getFailures()).isEmpty();
    }

    private Map<String, String> credentialServiceProperties() {
        var holderDid = WALLET.didFor(HOLDER).get();
        var credentials = WALLET.clientCredentials(holderDid);

        var correlationId = WALLET.createPendingCredentialRequest(holderDid, TCK_ISSUER_DID);

        return Map.of(
                "dataspacetck.did.holder", holderDid,
                "dataspacetck.sts.url", WALLET.tokenEndpoint().replaceFirst("/token$", ""),
                "dataspacetck.sts.client.id", credentials.clientId(),
                "dataspacetck.sts.client.secret", credentials.clientSecret(),
                "dataspacetck.credentials.correlation.id", correlationId,
                "dataspacetck.vc.scope.membershipcredential", "org.eclipse.dspace.dcp.vc.type:MembershipCredential:read",
                "dataspacetck.vc.scope.sensitivedatacredential", "org.eclipse.dspace.dcp.vc.type:SensitiveDataCredential:read"
        );
    }
}
