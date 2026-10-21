/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.spi.store;

import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.spi.persistence.StateEntityStore;
import org.eclipse.edc.spi.query.QuerySpec;

import java.util.stream.Stream;

/**
 * Stores states of data flow requests.
 */
public interface DataPlaneStore extends StateEntityStore<DataFlow> {

    /**
     * Returns all the data flows that match the given {@link QuerySpec}.
     *
     * @param querySpec the query to apply.
     * @return a stream of matching data flows, never null.
     */
    Stream<DataFlow> findAll(QuerySpec querySpec);

}
