/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.spi.transfer.flow;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.NotNull;

/**
 * Handles a data flow.
 */
public interface DataFlowController {

    /**
     * Returns true if the manager can handle the data type.
     */
    boolean canHandle(DataRequest dataRequest);

    /**
     * Initiate a data flow.
     * <p>Implementations should not throw exceptions. If an unexpected exception occurs and the flow should be re-attempted, set
     * {@link org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus#ERROR_RETRY} in the response. If an exception occurs and re-tries should not be re-attempted, set
     * {@link org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus#FATAL_ERROR} in the response. </p>
     */
    @NotNull
    DataFlowInitiateResult initiateFlow(DataRequest dataRequest);

}
