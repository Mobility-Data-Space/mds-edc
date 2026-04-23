package eu.dataspace.connector.agreements.retirement.store.sql;

import eu.dataspace.connector.agreements.retirement.spi.store.AgreementsRetirementStore;
import eu.dataspace.connector.agreements.retirement.store.AgreementsRetirementStoreTestBase;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.store.sql.contractnegotiation.store.SqlContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.store.sql.contractnegotiation.store.schema.ContractNegotiationStatements;
import org.eclipse.edc.connector.controlplane.store.sql.contractnegotiation.store.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.lease.BaseSqlLeaseStatements;
import org.eclipse.edc.sql.lease.SqlLeaseContextBuilderImpl;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.flywaydb.core.Flyway;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

@PostgresqlIntegrationTest
class SqlAgreementsRetirementStoreTest extends AgreementsRetirementStoreTestBase {
    private final TypeManager typeManager = new JacksonTypeManager();
    private final Clock clock = Clock.systemDefaultZone();
    private final SqlAgreementsRetirementStatements statements = new PostgresAgreementRetirementStatements();
    private final BaseSqlLeaseStatements leaseStatements = new BaseSqlLeaseStatements();
    private final ContractNegotiationStatements contractNegotiationStatements = new PostgresDialectStatements(leaseStatements, clock);
    private SqlContractNegotiationStore contractNegotiationStore;
    private SqlAgreementsRetirementStore store;

    @RegisterExtension
    static PostgresqlStoreSetupExtension extension =
            new PostgresqlStoreSetupExtension("postgres:18.1");

    @BeforeEach
    void setUp(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        contractNegotiationStore = createContractNegotiationStore(extension, queryExecutor);

        store = new SqlAgreementsRetirementStore(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), typeManager.getMapper(), queryExecutor, statements, contractNegotiationStatements);

        Flyway.configure()
                .baselineVersion("1.0.0")
                .baselineOnMigrate(true)
                .failOnMissingLocations(true)
                .dataSource(extension.getDataSourceRegistry().resolve(extension.getDatasourceName()))
                .table("flyway_schema_history")
                .locations("filesystem:" + TestUtils.findBuildRoot().toPath().resolve("extensions")
                        .resolve("database-schema-migration-connector").resolve("src").resolve("main")
                        .resolve("resources").resolve("migrations").resolve("connector"))
                .ignoreMigrationPatterns("*:ignored")
                .placeholders(Map.of("ParticipantContextId", UUID.randomUUID().toString()))
                .load()
                .migrate();
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("TRUNCATE TABLE " + statements.getTable() + " CASCADE");
        extension.runQuery("TRUNCATE TABLE " + contractNegotiationStatements.getContractAgreementTable() + " CASCADE");
    }

    @Override
    protected AgreementsRetirementStore getStore() {
        return store;
    }

    @Override
    protected ContractNegotiationStore getContractNegotiationStore() {
        return contractNegotiationStore;
    }

    private @NonNull SqlContractNegotiationStore createContractNegotiationStore(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        var leaseContextBuilder = SqlLeaseContextBuilderImpl.with(extension.getTransactionContext(), UUID.randomUUID().toString(), contractNegotiationStatements.getContractNegotiationTable(), leaseStatements, clock, queryExecutor);
        return new SqlContractNegotiationStore(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), typeManager.getMapper(), contractNegotiationStatements, leaseContextBuilder, queryExecutor);
    }
}
