package eu.dataspace.connector.agreements.retirement.store.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dataspace.connector.agreements.retirement.spi.store.AgreementsRetirementStore;
import eu.dataspace.connector.agreements.retirement.spi.types.AgreementsRetirementEntry;
import eu.dataspace.connector.agreements.retirement.spi.types.EnhancedContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.store.sql.contractnegotiation.store.schema.ContractNegotiationStatements;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.sql.translation.JsonFieldTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;
import org.eclipse.edc.sql.translation.TranslationMapping;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.stream.Stream;

public class SqlAgreementsRetirementStore extends AbstractSqlStore implements AgreementsRetirementStore {

    private final SqlAgreementsRetirementStatements agreementsRetirementStatements;
    private final ContractNegotiationStatements contractNegotiationStatements;

    public SqlAgreementsRetirementStore(DataSourceRegistry dataSourceRegistry, String dataSourceName,
                                        TransactionContext transactionContext, ObjectMapper objectMapper,
                                        QueryExecutor queryExecutor, SqlAgreementsRetirementStatements agreementsRetirementStatements,
                                        ContractNegotiationStatements contractNegotiationStatements) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.agreementsRetirementStatements = agreementsRetirementStatements;
        this.contractNegotiationStatements = contractNegotiationStatements;
    }

    @Override
    public StoreResult<Void> save(AgreementsRetirementEntry entry) {
        Objects.requireNonNull(entry);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (isRetired(entry.getAgreementId(), connection)) {
                    return StoreResult.alreadyExists(ALREADY_EXISTS_TEMPLATE.formatted(entry.getAgreementId()));
                }

                insert(connection, entry);
                return StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> delete(String contractAgreementId) {
        Objects.requireNonNull(contractAgreementId);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (!isRetired(contractAgreementId, connection)) {
                    return StoreResult.notFound(NOT_FOUND_IN_RETIREMENT_TEMPLATE.formatted(contractAgreementId));
                }
                queryExecutor.execute(connection, agreementsRetirementStatements.getDeleteByIdTemplate(), contractAgreementId);
                return StoreResult.success();
            } catch (Exception e) {
                throw new EdcPersistenceException(e.getMessage(), e);
            }
        });
    }

    @Override
    public Stream<AgreementsRetirementEntry> findRetiredAgreements(QuerySpec querySpec) {
        Objects.requireNonNull(querySpec);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var statement = agreementsRetirementStatements.createQuery(querySpec);
                return queryExecutor.query(connection, true, this::mapAgreementsRetirement, statement.getQueryAsString(), statement.getParameters());
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public Stream<EnhancedContractAgreement> findEnhancedAgreements(QuerySpec querySpec) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var query = contractNegotiationStatements.getSelectFromAgreementsTemplate() + " LEFT JOIN "
                        + agreementsRetirementStatements.getTable() + " ON "
                        + contractNegotiationStatements.getContractAgreementIdColumn() + " = " + agreementsRetirementStatements.getIdColumn();

                var statement = new SqlQueryStatement(query, querySpec, new ContractAgreementMapping(contractNegotiationStatements), agreementsRetirementStatements.getOperatorTranslator());

                return queryExecutor.query(connection, true, this::mapContractAgreementEnriched, statement.getQueryAsString(), statement.getParameters());
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private void insert(Connection conn, AgreementsRetirementEntry entry) {
        var insertStatement = agreementsRetirementStatements.insertTemplate();
        queryExecutor.execute(
                conn,
                insertStatement,
                entry.getAgreementId(),
                entry.getReason(),
                entry.getAgreementRetirementDate());
    }

    private boolean isRetired(String agreementId, Connection connection) {
        var sql = agreementsRetirementStatements.getCountByIdClause();
        try (var stream = queryExecutor.query(connection, false, this::mapRowCount, sql, agreementId)) {
            return stream.findFirst().orElse(0) > 0;
        }
    }

    private int mapRowCount(ResultSet resultSet) throws SQLException {
        return resultSet.getInt(agreementsRetirementStatements.getCountVariableName());
    }

    private EnhancedContractAgreement mapContractAgreementEnriched(ResultSet resultSet) throws SQLException {
        var retirement = mapAgreementsRetirement(resultSet);
        return new EnhancedContractAgreement(mapContractAgreement(resultSet), retirement);
    }

    private AgreementsRetirementEntry mapAgreementsRetirement(ResultSet resultSet) throws SQLException {
        var id = resultSet.getString(agreementsRetirementStatements.getIdColumn());
        if (id == null) {
            return null;
        }
        return AgreementsRetirementEntry.Builder.newInstance()
                .withAgreementId(id)
                .withReason(resultSet.getString(agreementsRetirementStatements.getReasonColumn()))
                .withAgreementRetirementDate(resultSet.getLong(agreementsRetirementStatements.getRetirementDateColumn()))
                .build();
    }

    private ContractAgreement mapContractAgreement(ResultSet resultSet) throws SQLException {
        return ContractAgreement.Builder.newInstance()
                .id(resultSet.getString(contractNegotiationStatements.getContractAgreementIdColumn()))
                .providerId(resultSet.getString(contractNegotiationStatements.getProviderAgentColumn()))
                .consumerId(resultSet.getString(contractNegotiationStatements.getConsumerAgentColumn()))
                .assetId(resultSet.getString(contractNegotiationStatements.getAssetIdColumn()))
                .contractSigningDate(resultSet.getLong(contractNegotiationStatements.getSigningDateColumn()))
                .policy(fromJson(resultSet.getString(contractNegotiationStatements.getPolicyColumn()), Policy.class))
                .participantContextId(resultSet.getString(contractNegotiationStatements.getAgreementParticipantContextIdColumn()))
                .agreementId(resultSet.getString(contractNegotiationStatements.getContractAgreementContractIdColumn()))
                .build();
    }

    // this class is not public in the EDC, but there should be a way to use it
    private static class ContractAgreementMapping extends TranslationMapping {

        public static final String FIELD_PARTICIPANT_CONTEXT_ID = "participantContextId";
        private static final String FIELD_ID = "id";
        private static final String FIELD_AGREEMENT_ID = "agreementId";
        private static final String FIELD_PROVIDER_AGENT_ID = "providerId";
        private static final String FIELD_CONSUMER_AGENT_ID = "consumerId";
        private static final String FIELD_CONTRACT_SIGNING_DATE = "contractSigningDate";
        private static final String FIELD_ASSET_ID = "assetId";
        private static final String FIELD_POLICY = "policy";

        ContractAgreementMapping(ContractNegotiationStatements statements) {
            add(FIELD_ID, statements.getContractAgreementIdColumn());
            add(FIELD_PROVIDER_AGENT_ID, statements.getProviderAgentColumn());
            add(FIELD_CONSUMER_AGENT_ID, statements.getConsumerAgentColumn());
            add(FIELD_CONTRACT_SIGNING_DATE, statements.getSigningDateColumn());
            add(FIELD_ASSET_ID, statements.getAssetIdColumn());
            add(FIELD_POLICY, new JsonFieldTranslator("policy"));
            add(FIELD_PARTICIPANT_CONTEXT_ID, statements.getAgreementParticipantContextIdColumn());
            add(FIELD_AGREEMENT_ID, statements.getContractAgreementContractIdColumn());
        }
    }
}
