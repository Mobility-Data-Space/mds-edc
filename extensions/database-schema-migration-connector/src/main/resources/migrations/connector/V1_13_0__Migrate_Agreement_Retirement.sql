-- EDC 1.0.0 natively supports contract agreement retirement: the retirement state is kept on the
-- agreement itself (edc_contract_agreement.retired/retirement_reason/retirement_date, added in V1_12_0)
-- instead of the separate edc_agreement_retirement table used by the former MDS retirement extension.
--
-- Copy every existing retirement entry onto its agreement, then drop the legacy table. Both store the
-- retirement date as epoch seconds. Entries pointing to agreements that do not exist are discarded:
-- they could never be created through the API and would have no effect.
UPDATE edc_contract_agreement agreement
SET retired           = TRUE,
    retirement_reason = retirement.reason,
    retirement_date   = retirement.agreement_retirement_date
FROM edc_agreement_retirement retirement
WHERE agreement.agr_id = retirement.contract_agreement_id;

DROP TABLE IF EXISTS edc_agreement_retirement;
