package eu.dataspace.connector.agreements.retirement.api;

import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;

import java.util.ArrayList;
import java.util.Map;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.query.Criterion.criterion;

/**
 * Translates a {@link QuerySpec} expressed on the legacy {@code AgreementsRetirementEntry} fields into a query on
 * retired {@link org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement}s.
 */
final class LegacyRetirementQuery {

    private static final Map<String, String> FIELDS = Map.of(
            "agreementId", "id",
            "reason", "retirementReason",
            "agreementRetirementDate", "retirementDate"
    );

    private LegacyRetirementQuery() {
    }

    static QuerySpec toContractAgreementQuery(QuerySpec legacy) {
        var filters = new ArrayList<Criterion>();
        filters.add(criterion("retired", "=", true));
        legacy.getFilterExpression().stream()
                .map(it -> new Criterion(mapField(it.getOperandLeft()), it.getOperator(), it.getOperandRight()))
                .forEach(filters::add);

        return QuerySpec.Builder.newInstance()
                .offset(legacy.getOffset())
                .limit(legacy.getLimit())
                .sortOrder(legacy.getSortOrder())
                .sortField(legacy.getSortField() == null ? null : mapField(legacy.getSortField()).toString())
                .filter(filters)
                .build();
    }

    private static Object mapField(Object field) {
        if (field instanceof String name) {
            var shortName = name.startsWith(EDC_NAMESPACE) ? name.substring(EDC_NAMESPACE.length()) : name;
            return FIELDS.getOrDefault(shortName, shortName);
        }
        return field;
    }
}
