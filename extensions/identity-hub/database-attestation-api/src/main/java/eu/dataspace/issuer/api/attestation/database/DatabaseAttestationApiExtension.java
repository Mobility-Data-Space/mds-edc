/*
 * Copyright (c) 2025 Mobility Data Space
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *      Think-it GmbH - initial API and implementation
 */

package eu.dataspace.issuer.api.attestation.database;

import org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.web.spi.WebService;

// TODO: replace this with holder attestation (see https://github.com/eclipse-edc/IdentityHub/issues/851)
public class DatabaseAttestationApiExtension implements ServiceExtension {

    @Inject
    private WebService webService;
    @Inject
    private TypeManager typeManager;
    @Inject
    private DataSourceRegistry dataSourceRegistry;
    @Inject
    private QueryExecutor queryExecutor;
    @Inject
    private TransactionContext transactionContext;

    @Override
    public String name() {
        return "Database Attestation Api";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var store = new DatabaseAttestationSourceStore("default", typeManager.getMapper(), dataSourceRegistry, queryExecutor, transactionContext);

        webService.registerResource(IdentityHubApiContext.ISSUERADMIN, new DatabaseAttestationApiController(store));
    }
}
